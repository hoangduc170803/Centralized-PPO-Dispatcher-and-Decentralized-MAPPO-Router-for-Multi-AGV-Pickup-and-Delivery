"""Adapter: PettingZoo `WarehouseEnv` -> marlbenchmark/on-policy interface.

on-policy runners (e.g. ``SMACRunner``) expect every env to expose
``observation_space``, ``share_observation_space``, ``action_space`` as
*lists* of length ``num_agents``, plus a Gym-like reset/step that returns
numpy arrays of shape ``(num_agents, ...)``.

The warehouse PettingZoo env returns per-agent dicts keyed by agent name.
This module wraps it in the shape on-policy wants:

    reset()  -> (obs, share_obs, available_actions)
        obs[i]              shape (obs_dim,)
        share_obs[i]        shape (share_obs_dim,)  -- same vector for all i
        available_actions[i] shape (num_actions,)

    step(actions) -> (obs, share_obs, rewards, dones, infos, available_actions)
        actions             shape (num_agents, 1)   int discrete index
        rewards             shape (num_agents, 1)
        dones               shape (num_agents,)     bool

``share_obs`` for Sprint 3 is the concatenation of every agent's local
observation. This matches the limitation of the legacy ``mappo_router``
prototype and keeps the comparison fair. Sprint 4 will swap this for a
richer global state that also includes pending tasks.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any, Optional

import networkx as nx
import numpy as np
from gymnasium import spaces

from src.env.compass_mapper import NUM_ACTIONS
from src.env.warehouse_env import WarehouseEnv
from src.map_parser import parse_opentcs_map
from src.routing.astar import AStarRouter


DEFAULT_MAP_FILE = (
    Path(__file__).resolve().parents[3]
    / "orca_share_media1778260607027_7458565577098821053.xml"
)


@dataclass
class WarehouseEnvConfig:
    """Subset of `WarehouseEnv` knobs needed by the on-policy training loop."""

    map_path: Path = DEFAULT_MAP_FILE
    num_agents: int = 5
    episode_horizon: int = 128
    task_rate: float = 0.1
    knn_agents: int = 3
    seed: int = 0


def build_warehouse_env(cfg: WarehouseEnvConfig) -> WarehouseEnv:
    """Construct the largest-SCC warehouse env used for MAPPO training."""
    graph = parse_opentcs_map(str(cfg.map_path), restrict_to_largest_scc=True)
    router = AStarRouter(graph, precompute=True)
    return WarehouseEnv(
        graph=graph,
        router=router,
        num_agents=cfg.num_agents,
        episode_horizon=cfg.episode_horizon,
        task_rate=cfg.task_rate,
        knn_agents=cfg.knn_agents,
        seed=cfg.seed,
    )


class WarehouseOnPolicyEnv:
    """on-policy compatible wrapper around `WarehouseEnv` parallel API.

    Parameters
    ----------
    env_config:
        Construction args for `WarehouseEnv`. The wrapper owns the env.
    auto_reset:
        When ``True`` (default), the wrapper resets the env automatically on
        truncation so the runner can keep collecting transitions without
        special-casing the boundary. ``done`` for the terminal step is still
        propagated so masks/value bootstrap are correct.
    """

    metadata = {"render_modes": []}

    def __init__(
        self,
        env_config: WarehouseEnvConfig | None = None,
        env: Optional[WarehouseEnv] = None,
        auto_reset: bool = True,
    ):
        if env is None:
            if env_config is None:
                env_config = WarehouseEnvConfig()
            env = build_warehouse_env(env_config)
        self.env = env
        self.cfg = env_config
        self.auto_reset = auto_reset

        sample_agent = self.env.possible_agents[0]
        per_agent_obs_space = self.env.observation_space(sample_agent)
        obs_box = per_agent_obs_space["observation"]
        self.obs_dim = int(obs_box.shape[0])
        self.num_actions = NUM_ACTIONS
        self.num_agents = len(self.env.possible_agents)

        per_agent_obs = spaces.Box(
            low=-np.inf,
            high=np.inf,
            shape=(self.obs_dim,),
            dtype=np.float32,
        )
        per_agent_share = spaces.Box(
            low=-np.inf,
            high=np.inf,
            shape=(self.obs_dim * self.num_agents,),
            dtype=np.float32,
        )
        per_agent_action = spaces.Discrete(self.num_actions)

        self.observation_space = [per_agent_obs for _ in range(self.num_agents)]
        self.share_observation_space = [
            per_agent_share for _ in range(self.num_agents)
        ]
        self.action_space = [per_agent_action for _ in range(self.num_agents)]
        self._agent_order = list(self.env.possible_agents)

        self._latest_infos: dict[str, Any] = {}

    @property
    def agent_order(self) -> list[str]:
        return list(self._agent_order)

    def _stack_obs(
        self, obs: dict[str, dict[str, np.ndarray]]
    ) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
        per_agent_obs = np.stack(
            [obs[a]["observation"].astype(np.float32) for a in self._agent_order],
            axis=0,
        )
        avail = np.stack(
            [obs[a]["action_mask"].astype(np.float32) for a in self._agent_order],
            axis=0,
        )
        flat = per_agent_obs.reshape(-1)
        share = np.broadcast_to(flat, (self.num_agents, flat.shape[0])).astype(
            np.float32,
            copy=True,
        )
        return per_agent_obs, share, avail

    def reset(
        self, seed: Optional[int] = None
    ) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
        obs, _ = self.env.reset(seed=seed)
        per_agent, share, avail = self._stack_obs(obs)
        self._latest_infos = {}
        return per_agent, share, avail

    def step(
        self,
        actions: np.ndarray,
    ) -> tuple[
        np.ndarray, np.ndarray, np.ndarray, np.ndarray, dict[str, Any], np.ndarray
    ]:
        # ``actions`` from on-policy is shape (num_agents, 1) float. Convert
        # to a dict[name, int] for the PettingZoo parallel API.
        flat_actions = np.asarray(actions).reshape(-1).astype(np.int64)
        if flat_actions.shape[0] != self.num_agents:
            raise ValueError(
                f"expected {self.num_agents} actions, got {flat_actions.shape[0]}"
            )
        action_dict = {
            agent: int(flat_actions[i]) for i, agent in enumerate(self._agent_order)
        }
        obs, rewards, terminated, truncated, infos = self.env.step(action_dict)

        per_agent_obs, share_obs, avail = self._stack_obs(obs)
        rewards_arr = np.array(
            [[float(rewards[a])] for a in self._agent_order], dtype=np.float32
        )
        dones_arr = np.array(
            [bool(terminated[a] or truncated[a]) for a in self._agent_order],
            dtype=bool,
        )

        first_info = next(iter(infos.values()), {})
        merged_info = dict(first_info)
        merged_info["bad_transition"] = bool(
            all(truncated.values()) and not all(terminated.values())
        )
        self._latest_infos = merged_info

        if self.auto_reset and dones_arr.all():
            # Reset BEFORE returning so the next collect() reads valid obs,
            # but keep dones=True so the runner masks the value bootstrap.
            reset_obs, reset_share, reset_avail = self.reset()
            per_agent_obs, share_obs, avail = reset_obs, reset_share, reset_avail

        return per_agent_obs, share_obs, rewards_arr, dones_arr, merged_info, avail

    def seed(self, seed: Optional[int]) -> None:
        if seed is not None:
            self.env.reset(seed=seed)

    def close(self) -> None:
        self.env.close()

    @property
    def latest_info(self) -> dict[str, Any]:
        return dict(self._latest_infos)
