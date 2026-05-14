"""Minimal MAPPO trainer for the Sprint 3 vanilla MLP router prototype."""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
import torch
from torch import nn

from src.rl.mappo_router.buffer import RolloutBuffer
from src.rl.mappo_router.models import (
    CentralizedCritic,
    SharedActor,
    build_global_state,
    stack_agent_observations,
)
from src.utils.episode_logger import EpisodeLogger


DEFAULT_MAP_FILE = (
    Path(__file__).resolve().parents[3]
    / "orca_share_media1778260607027_7458565577098821053.xml"
)


@dataclass
class MAPPOConfig:
    """Hyperparameters for a small MAPPO update loop."""

    num_steps: int = 128
    update_epochs: int = 4
    gamma: float = 0.99
    gae_lambda: float = 0.95
    clip_coef: float = 0.2
    ent_coef: float = 0.01
    vf_coef: float = 0.5
    max_grad_norm: float = 0.5
    learning_rate: float = 3e-4
    hidden_dim: int = 128
    deterministic_eval: bool = False
    seed: int = 0
    device: str = "cpu"


class MAPPOTrainer:
    """Team-value MAPPO-lite trainer for `WarehouseEnv` parallel API."""

    def __init__(
        self,
        env,
        config: MAPPOConfig | None = None,
    ):
        self.env = env
        self.config = config or MAPPOConfig()
        self.device = torch.device(self.config.device)
        torch.manual_seed(self.config.seed)
        np.random.seed(self.config.seed)

        sample_agent = env.possible_agents[0]
        obs_dim = int(env.observation_space(sample_agent)["observation"].shape[0])
        num_actions = int(env.action_space(sample_agent).n)
        self.num_agents = len(env.possible_agents)
        self.global_state_dim = obs_dim * self.num_agents

        self.actor = SharedActor(obs_dim, num_actions, hidden_dim=self.config.hidden_dim).to(
            self.device
        )
        self.critic = CentralizedCritic(
            self.global_state_dim,
            hidden_dim=self.config.hidden_dim,
        ).to(self.device)
        self.optimizer = torch.optim.Adam(
            list(self.actor.parameters()) + list(self.critic.parameters()),
            lr=self.config.learning_rate,
            eps=1e-5,
        )
        self._obs: dict[str, dict[str, np.ndarray]] | None = None
        self._agent_order = list(env.possible_agents)

    def reset(self, seed: int | None = None) -> dict[str, dict[str, np.ndarray]]:
        obs, _ = self.env.reset(seed=seed)
        self._obs = obs
        return obs

    def train_updates(self, num_updates: int = 1) -> list[dict[str, float]]:
        if num_updates < 1:
            raise ValueError("num_updates must be >= 1")
        if self._obs is None:
            self.reset(seed=self.config.seed)

        metrics: list[dict[str, float]] = []
        for _ in range(num_updates):
            buffer, rollout_metrics = self.collect_rollout()
            next_value = self._current_value()
            batch = buffer.as_batch(
                next_value=next_value,
                gamma=self.config.gamma,
                gae_lambda=self.config.gae_lambda,
                device=self.device,
            )
            update_metrics = self.update(batch)
            metrics.append({**rollout_metrics, **update_metrics})
        return metrics

    def collect_rollout(self) -> tuple[RolloutBuffer, dict[str, float]]:
        if self._obs is None:
            self.reset(seed=self.config.seed)
        assert self._obs is not None

        buffer = RolloutBuffer()
        logger = EpisodeLogger(num_agents=self.num_agents)
        completed_total = 0

        for _ in range(self.config.num_steps):
            obs = self._obs
            agents, obs_tensor, mask_tensor = stack_agent_observations(
                obs,
                self._agent_order,
                device=self.device,
            )
            global_state = build_global_state(obs, agents, device=self.device)
            with torch.no_grad():
                actions, log_probs, _ = self.actor.act(
                    obs_tensor,
                    mask_tensor,
                    deterministic=self.config.deterministic_eval,
                )
                value = self.critic(global_state.unsqueeze(0)).squeeze(0)

            action_dict = {
                agent: int(action.item())
                for agent, action in zip(agents, actions)
            }
            next_obs, rewards, terminated, truncated, infos = self.env.step(action_dict)
            done = all(terminated.values()) or all(truncated.values())
            team_reward = float(np.mean([float(rewards[agent]) for agent in agents]))
            logger.record_step(action_dict, rewards, infos, active_agents=len(agents))
            if infos:
                completed_total = int(
                    next(iter(infos.values())).get("tasks_completed_total", completed_total)
                )

            buffer.add(
                obs=obs_tensor,
                action_mask=mask_tensor,
                global_state=global_state,
                actions=actions,
                log_probs=log_probs,
                reward=team_reward,
                done=done,
                value=value,
            )

            self._obs = next_obs
            if done:
                self.reset()

        episode_metrics = logger.finalize(extra={"algo": "mappo_mlp"})
        return buffer, {
            "rollout_reward_mean": float(np.mean(buffer.rewards)),
            "rollout_tasks_completed": float(completed_total),
            "rollout_validator_interventions": float(episode_metrics.validator_interventions),
            "rollout_waiting_time_agent_steps": float(
                episode_metrics.waiting_time_agent_steps
            ),
        }

    def update(self, batch) -> dict[str, float]:
        cfg = self.config
        num_steps, num_agents = batch.actions.shape
        flat_obs = batch.obs.reshape(num_steps * num_agents, -1)
        flat_masks = batch.action_masks.reshape(num_steps * num_agents, -1)
        flat_actions = batch.actions.reshape(num_steps * num_agents)
        flat_old_log_probs = batch.old_log_probs.reshape(num_steps * num_agents)
        flat_advantages = batch.advantages[:, None].expand(num_steps, num_agents).reshape(-1)

        last_metrics: dict[str, float] = {}
        for _ in range(cfg.update_epochs):
            new_log_probs, entropy = self.actor.evaluate_actions(
                flat_obs,
                flat_masks,
                flat_actions,
            )
            log_ratio = new_log_probs - flat_old_log_probs
            ratio = log_ratio.exp()
            policy_loss_1 = -flat_advantages * ratio
            policy_loss_2 = -flat_advantages * torch.clamp(
                ratio,
                1.0 - cfg.clip_coef,
                1.0 + cfg.clip_coef,
            )
            policy_loss = torch.max(policy_loss_1, policy_loss_2).mean()

            values = self.critic(batch.global_states)
            value_loss = 0.5 * (batch.returns - values).pow(2).mean()
            entropy_loss = entropy.mean()
            loss = policy_loss + cfg.vf_coef * value_loss - cfg.ent_coef * entropy_loss

            self.optimizer.zero_grad()
            loss.backward()
            nn.utils.clip_grad_norm_(
                list(self.actor.parameters()) + list(self.critic.parameters()),
                cfg.max_grad_norm,
            )
            self.optimizer.step()

            with torch.no_grad():
                approx_kl = ((ratio - 1.0) - log_ratio).mean().clamp_min(0.0)
            last_metrics = {
                "loss_total": float(loss.detach().cpu()),
                "loss_policy": float(policy_loss.detach().cpu()),
                "loss_value": float(value_loss.detach().cpu()),
                "entropy": float(entropy_loss.detach().cpu()),
                "approx_kl": float(approx_kl.detach().cpu()),
            }

        return last_metrics

    def _current_value(self) -> torch.Tensor:
        if self._obs is None:
            return torch.tensor(0.0, device=self.device)
        global_state = build_global_state(self._obs, self._agent_order, device=self.device)
        with torch.no_grad():
            return self.critic(global_state.unsqueeze(0)).squeeze(0)


def train_smoke(env, config: MAPPOConfig | None = None) -> dict[str, Any]:
    """Run one small MAPPO update and return metrics for tests/debug scripts."""
    trainer = MAPPOTrainer(env, config=config)
    return trainer.train_updates(num_updates=1)[0]


def make_warehouse_env(
    *,
    map_path: Path = DEFAULT_MAP_FILE,
    num_agents: int = 5,
    horizon: int = 256,
    task_rate: float = 0.1,
    seed: int = 0,
):
    """Build the default largest-SCC warehouse env used by MAPPO smoke runs."""
    from src.env.warehouse_env import WarehouseEnv
    from src.map_parser import parse_opentcs_map
    from src.routing.astar import AStarRouter

    graph = parse_opentcs_map(str(map_path), restrict_to_largest_scc=True)
    router = AStarRouter(graph, precompute=True)
    return WarehouseEnv(
        graph=graph,
        router=router,
        num_agents=num_agents,
        episode_horizon=horizon,
        task_rate=task_rate,
        seed=seed,
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--map", type=Path, default=DEFAULT_MAP_FILE)
    parser.add_argument("--agents", type=int, default=5)
    parser.add_argument("--horizon", type=int, default=256)
    parser.add_argument("--task-rate", type=float, default=0.1)
    parser.add_argument("--updates", type=int, default=1)
    parser.add_argument("--steps", type=int, default=128)
    parser.add_argument("--epochs", type=int, default=4)
    parser.add_argument("--hidden-dim", type=int, default=128)
    parser.add_argument("--learning-rate", type=float, default=3e-4)
    parser.add_argument("--seed", type=int, default=0)
    parser.add_argument("--device", default="cpu")
    args = parser.parse_args(argv)

    env = make_warehouse_env(
        map_path=args.map,
        num_agents=args.agents,
        horizon=args.horizon,
        task_rate=args.task_rate,
        seed=args.seed,
    )
    config = MAPPOConfig(
        num_steps=args.steps,
        update_epochs=args.epochs,
        hidden_dim=args.hidden_dim,
        learning_rate=args.learning_rate,
        seed=args.seed,
        device=args.device,
    )
    trainer = MAPPOTrainer(env, config=config)
    metrics = trainer.train_updates(num_updates=args.updates)
    print(json.dumps(metrics, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
