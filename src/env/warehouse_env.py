"""PettingZoo parallel env: warehouse multi-AGV pickup-and-delivery.

Sprint 1 scope:
  - flat MLP-friendly observation (no GNN yet — that's Sprint 4)
  - per-agent compass action space (size 9) with mask
  - safety validator overlay
  - simple greedy dispatcher inside env (Hungarian comes in Sprint 2)
  - Poisson task spawning

Per-agent observation is a `Dict` of:
  - "observation": float32 vector, see `_build_obs`
  - "action_mask": int8 vector of length 9 (compass + WAIT)

Episode terminates when `episode_horizon` steps elapse (truncation).
There is no agent-level termination — even idle agents stay alive.
"""

from __future__ import annotations

import math
from dataclasses import dataclass, field, replace
from enum import IntEnum
from typing import Any, Optional

import networkx as nx
import numpy as np
from gymnasium import spaces
from pettingzoo.utils.env import ParallelEnv

from .compass_mapper import NUM_ACTIONS, WAIT_SLOT, CompassMapper
from .reward_shaper import AgentRewardSignal, RewardConfig, compute_reward
from .safety_validator import SafetyValidator
from .task_generator import PoissonTaskGenerator, Task


class AgentState(IntEnum):
    IDLE = 0
    TO_PICKUP = 1
    TO_DROPOFF = 2


@dataclass
class AgentInfo:
    """Mutable per-agent state held by the env."""

    name: str
    pos: str
    state: AgentState = AgentState.IDLE
    task: Optional[Task] = None
    age: float = 0.0          # steps since last move (for validator priority)
    age_idle: float = 0.0     # steps spent IDLE (for dispatch fairness)
    prev_pos: str = field(default="")  # used for following-conflict detection
    last_dist_to_goal: float = 0.0


class WarehouseEnv(ParallelEnv):
    """5-AGV (default) PettingZoo parallel env."""

    metadata = {"render_modes": [], "name": "warehouse_v0"}

    def __init__(
        self,
        graph: nx.DiGraph,
        router,                    # AStarRouter, ideally precomputed
        num_agents: int = 5,
        episode_horizon: int = 512,
        task_rate: float = 0.1,
        knn_agents: int = 3,
        max_tasks_in_pool: int = 50,
        validator_check_following: bool = True,
        reward_config: Optional[RewardConfig] = None,
        seed: Optional[int] = None,
    ):
        super().__init__()
        if num_agents < 1:
            raise ValueError("num_agents must be >= 1")
        if knn_agents >= num_agents:
            knn_agents = max(0, num_agents - 1)

        self.G = graph
        self.router = router
        self.num_agents_target = num_agents
        self.episode_horizon = episode_horizon
        self.task_rate = task_rate
        self.knn = knn_agents
        self.max_tasks_in_pool = max_tasks_in_pool

        self.compass = CompassMapper(self.G)
        self.validator = SafetyValidator(check_following=validator_check_following)

        nodes = list(self.G.nodes())
        self._nodes = nodes
        self._node_pool = nodes
        xs = np.array([self.G.nodes[n]["x"] for n in nodes])
        ys = np.array([self.G.nodes[n]["y"] for n in nodes])
        self._x_min, self._x_max = float(xs.min()), float(xs.max())
        self._y_min, self._y_max = float(ys.min()), float(ys.max())
        if router._dist_matrix is not None:
            finite = router._dist_matrix[np.isfinite(router._dist_matrix)]
            self._max_dist = float(finite.max()) if finite.size else 1.0
        else:
            # Lazy router can't tell us the graph diameter without paying the
            # full all-pairs cost it was meant to avoid. Any cheap heuristic
            # (bbox diagonal, sum of edges) is either not a real upper bound or
            # is too loose to be useful — so we don't pretend to normalize.
            # Training MUST use precompute=True; this path exists for debug only.
            import warnings

            warnings.warn(
                "WarehouseEnv was given a lazy AStarRouter (precompute=False). "
                "Distance normalizer falls back to 1.0, so d_norm in observations "
                "and the Δ(A*_dist) reward term are NOT properly scaled. Pass "
                "AStarRouter(G, precompute=True) for any training run.",
                RuntimeWarning,
                stacklevel=2,
            )
            self._max_dist = 1.0

        # Reward config: copy user's so we don't mutate it, then bind the
        # distance normalizer so progress shaping lives on a [-1, 1] scale.
        base_cfg = reward_config or RewardConfig()
        self.cfg = replace(base_cfg, dist_normalizer=self._max_dist)

        self.possible_agents = [f"agv_{i}" for i in range(num_agents)]
        self.agents: list[str] = []
        self._agent_info: dict[str, AgentInfo] = {}
        self._step_idx: int = 0
        self._task_gen: Optional[PoissonTaskGenerator] = None
        self._rng = np.random.default_rng(seed)

        self._obs_dim = self._compute_obs_dim()

    # ------------------------------------------------------------------ spaces

    def _compute_obs_dim(self) -> int:
        # self pos (2) + goal pos (2) + dist_to_goal (1) + astar one-hot (9)
        # + agent state one-hot (3) + KNN agents (3 each: dx, dy, has_task)
        return 2 + 2 + 1 + NUM_ACTIONS + 3 + 3 * self.knn

    def observation_space(self, agent: str) -> spaces.Dict:
        return spaces.Dict(
            {
                "observation": spaces.Box(
                    low=-np.inf, high=np.inf, shape=(self._obs_dim,), dtype=np.float32
                ),
                "action_mask": spaces.Box(
                    low=0, high=1, shape=(NUM_ACTIONS,), dtype=np.int8
                ),
            }
        )

    def action_space(self, agent: str) -> spaces.Discrete:
        return spaces.Discrete(NUM_ACTIONS)

    # ------------------------------------------------------------------ helpers

    def _norm_xy(self, node: str) -> tuple[float, float]:
        x = (self.G.nodes[node]["x"] - self._x_min) / max(self._x_max - self._x_min, 1e-9)
        y = (self.G.nodes[node]["y"] - self._y_min) / max(self._y_max - self._y_min, 1e-9)
        return float(x), float(y)

    def _astar_hint_onehot(self, current: str, goal: Optional[str]) -> np.ndarray:
        oh = np.zeros(NUM_ACTIONS, dtype=np.float32)
        if goal is None or goal == current:
            oh[WAIT_SLOT] = 1.0
            return oh
        nh = self.router.next_hop(current, goal)
        if nh is None:
            oh[WAIT_SLOT] = 1.0
            return oh
        _, slot_to_nbr = self.compass.get(current)
        for slot, nbr in slot_to_nbr.items():
            if nbr == nh:
                oh[slot] = 1.0
                return oh
        oh[WAIT_SLOT] = 1.0
        return oh

    def _knn_features(self, me: str) -> np.ndarray:
        my_pos = self._agent_info[me].pos
        my_x, my_y = self._norm_xy(my_pos)
        others = []
        for other_name, info in self._agent_info.items():
            if other_name == me:
                continue
            ox, oy = self._norm_xy(info.pos)
            others.append((ox - my_x, oy - my_y, 1.0 if info.task is not None else 0.0))
        # Sort by L2 distance ascending
        others.sort(key=lambda t: t[0] * t[0] + t[1] * t[1])
        out = np.zeros(3 * self.knn, dtype=np.float32)
        for i, feat in enumerate(others[: self.knn]):
            out[3 * i : 3 * i + 3] = feat
        return out

    def _build_obs(self, agent: str) -> dict:
        info = self._agent_info[agent]
        goal = info.task.current_goal if info.task is not None else None
        self_x, self_y = self._norm_xy(info.pos)
        if goal is not None:
            gx, gy = self._norm_xy(goal)
            d = self.router.distance(info.pos, goal)
            d_norm = min(d / self._max_dist, 1.0) if math.isfinite(d) else 1.0
        else:
            gx = gy = 0.0
            d_norm = 0.0
        astar_oh = self._astar_hint_onehot(info.pos, goal)

        state_oh = np.zeros(3, dtype=np.float32)
        state_oh[int(info.state)] = 1.0

        knn = self._knn_features(agent)
        flat = np.concatenate(
            [
                np.array([self_x, self_y, gx, gy, d_norm], dtype=np.float32),
                astar_oh,
                state_oh,
                knn,
            ]
        )
        mask, _ = self.compass.get(info.pos)
        return {"observation": flat, "action_mask": mask.copy()}

    # ------------------------------------------------------------------ dispatch

    def _greedy_dispatch(self):
        """Assign pending tasks to IDLE agents, nearest pickup wins.

        Replace with Hungarian (Sprint 2) or learned dispatcher (Sprint 5).
        """
        idle = [info for info in self._agent_info.values() if info.task is None]
        if not idle or self._task_gen is None or not self._task_gen.pending:
            return
        # Sort idle agents by idle-age descending so long-waiting agents go first.
        idle.sort(key=lambda a: -a.age_idle)
        for agent in idle:
            if not self._task_gen.pending:
                break
            best_task = None
            best_d = math.inf
            for task in self._task_gen.pending:
                d = self.router.distance(agent.pos, task.pickup)
                if d < best_d:
                    best_d = d
                    best_task = task
            if best_task is None or not math.isfinite(best_d):
                continue
            self._task_gen.assign(best_task, agent.name)
            agent.task = best_task
            agent.state = AgentState.TO_PICKUP
            agent.age_idle = 0.0
            agent.last_dist_to_goal = best_d

    # ------------------------------------------------------------------ reset / step

    def reset(self, seed: Optional[int] = None, options: Optional[dict] = None):
        if seed is not None:
            self._rng = np.random.default_rng(seed)
        self._step_idx = 0
        self.agents = list(self.possible_agents)
        self._task_gen = PoissonTaskGenerator(
            node_pool=self._node_pool,
            rate=self.task_rate,
            max_pool_size=self.max_tasks_in_pool,
            rng=self._rng,
        )

        # Place agents on distinct random nodes
        start_nodes = self._rng.choice(
            self._node_pool, size=self.num_agents_target, replace=False
        )
        self._agent_info.clear()
        for name, node in zip(self.possible_agents, start_nodes):
            self._agent_info[name] = AgentInfo(
                name=name, pos=str(node), prev_pos=str(node)
            )

        # Pre-seed the task pool so agents have work from t=0
        seed_count = max(2 * self.num_agents_target, 4)
        for _ in range(seed_count):
            if self._task_gen.num_pending() + self._task_gen.num_in_flight() >= self.max_tasks_in_pool:
                break
            pickup, dropoff = self._rng.choice(self._node_pool, size=2, replace=False)
            self._task_gen.pending.append(
                Task(
                    id=self._task_gen._next_id,
                    pickup=str(pickup),
                    dropoff=str(dropoff),
                    spawn_step=0,
                )
            )
            self._task_gen._next_id += 1

        self._greedy_dispatch()

        obs = {a: self._build_obs(a) for a in self.agents}
        infos = {a: {} for a in self.agents}
        return obs, infos

    def step(self, actions: dict[str, int]):
        self._step_idx += 1
        positions = [self._agent_info[a].pos for a in self.agents]
        slot_maps = [self.compass.get(p)[1] for p in positions]
        masks = [self.compass.get(p)[0] for p in positions]

        # Capture raw action signals before validator. Defensive against
        # out-of-range actions (e.g., a buggy wrapper or random debug code
        # producing -1 / 99) — clamp them to WAIT instead of letting the env
        # crash with IndexError when indexing into the per-agent mask. The
        # original out-of-range request is still reported as invalid so the
        # policy gets a learning signal for misbehaving.
        raw_action_input = np.array(
            [int(actions.get(a, WAIT_SLOT)) for a in self.agents], dtype=np.int64
        )
        out_of_range = (raw_action_input < 0) | (raw_action_input >= NUM_ACTIONS)
        # `raw_was_wait` must reflect what the policy ACTUALLY chose — sampling
        # an out-of-range action is a malformed request, not "choosing WAIT".
        # Computing this after clamping would double-penalize: r_invalid_action
        # AND r_unnecessary_wait for the same mistake.
        raw_was_wait = raw_action_input == WAIT_SLOT
        raw_action = np.where(out_of_range, WAIT_SLOT, raw_action_input)

        invalid_flags = np.array(
            [masks[i][raw_action[i]] == 0 for i in range(len(self.agents))], dtype=bool
        )
        invalid_flags = invalid_flags | out_of_range
        had_valid_move = np.array(
            [int(masks[i].sum()) > 1 for i in range(len(self.agents))], dtype=bool
        )

        # If invalid, force WAIT before validator
        sanitized = np.where(invalid_flags, WAIT_SLOT, raw_action)

        # Snapshot the per-agent goal BEFORE any move or state transition
        # happens. We use this same goal for both `dist_prev` and `dist_now`
        # so the potential-based shaping `Δ(A*_dist)` telescopes correctly:
        # at the pickup step, dist_now = distance(new_pos == pickup, pickup) = 0,
        # giving a small positive delta instead of the huge spike you'd get
        # from comparing dist-to-pickup with dist-to-dropoff (different goals).
        # Mirrors the dropoff transition: snapshot goal stays the dropoff,
        # `info.task` may already be cleared by the transition further down.
        goal_snapshot: list[Optional[str]] = []
        for a in self.agents:
            t = self._agent_info[a].task
            goal_snapshot.append(t.current_goal if t is not None else None)

        # Distances to goal BEFORE moving (for progress reward & validator tiebreaker)
        dist_prev = np.zeros(len(self.agents), dtype=np.float32)
        for i, a in enumerate(self.agents):
            info = self._agent_info[a]
            goal = goal_snapshot[i]
            if goal is None:
                dist_prev[i] = 0.0
            else:
                dist_prev[i] = self.router.distance(info.pos, goal)

        ages = np.array([self._agent_info[a].age for a in self.agents], dtype=np.float32)
        safe_action, report = self.validator.validate(
            positions=positions,
            joint_action=sanitized,
            slot_maps=slot_maps,
            distances_to_goal=dist_prev,
            ages=ages,
        )

        # Apply safe actions
        for i, a in enumerate(self.agents):
            info = self._agent_info[a]
            info.prev_pos = info.pos
            nxt = slot_maps[i].get(int(safe_action[i]), info.pos)
            if nxt != info.pos:
                info.pos = nxt
                info.age = 0.0
            else:
                info.age += 1.0

        # Task progress
        reached_pickup = {a: False for a in self.agents}
        reached_dropoff = {a: False for a in self.agents}
        for a in self.agents:
            info = self._agent_info[a]
            if info.task is None:
                continue
            if info.state == AgentState.TO_PICKUP and info.pos == info.task.pickup:
                self._task_gen.mark_picked_up(info.task.id)
                info.task.picked_up = True
                info.state = AgentState.TO_DROPOFF
                reached_pickup[a] = True
            elif info.state == AgentState.TO_DROPOFF and info.pos == info.task.dropoff:
                self._task_gen.complete(info.task.id, self._step_idx)
                info.task = None
                info.state = AgentState.IDLE
                info.age_idle = 0.0
                reached_dropoff[a] = True

        # Spawn + re-dispatch idle agents
        self._task_gen.step(self._step_idx)
        for a in self.agents:
            info = self._agent_info[a]
            if info.task is None:
                info.age_idle += 1.0
        self._greedy_dispatch()

        # Distances AFTER moving for reward shaping. Use the snapshot goal
        # captured at the start of the step so progress shaping telescopes
        # correctly across pickup/dropoff transitions (see snapshot block above).
        dist_now = np.zeros(len(self.agents), dtype=np.float32)
        for i, a in enumerate(self.agents):
            info = self._agent_info[a]
            goal = goal_snapshot[i]
            if goal is None:
                dist_now[i] = 0.0
            else:
                dist_now[i] = self.router.distance(info.pos, goal)
            info.last_dist_to_goal = float(dist_now[i])

        forced_to_wait = (safe_action != sanitized)

        rewards: dict[str, float] = {}
        for i, a in enumerate(self.agents):
            info = self._agent_info[a]
            sig = AgentRewardSignal(
                reached_pickup=reached_pickup[a],
                reached_dropoff=reached_dropoff[a],
                raw_action_was_wait=bool(raw_was_wait[i]),
                had_valid_move=bool(had_valid_move[i]),
                forced_to_wait=bool(forced_to_wait[i]),
                invalid_action_requested=bool(invalid_flags[i]),
                # The validator already prevents these from materializing in
                # the env transition, but we propagate the FIRST-PASS conflict
                # flags so the policy gets a learning signal for raw actions
                # that *would* have collided.
                in_vertex_conflict=bool(report.vertex_flags[i]),
                in_edge_swap=bool(report.edge_swap_flags[i]),
                in_following_conflict=bool(report.following_flags[i]),
                dist_to_goal_prev=float(dist_prev[i]),
                dist_to_goal_now=float(dist_now[i]),
                # Use the snapshot rather than `info.task is not None` so the
                # agent still gets the final-segment progress credit at the
                # dropoff step (where info.task has just been cleared above).
                has_active_task=goal_snapshot[i] is not None,
            )
            rewards[a] = compute_reward(sig, self.cfg)

        terminated = {a: False for a in self.agents}
        truncated = {a: self._step_idx >= self.episode_horizon for a in self.agents}

        obs = {a: self._build_obs(a) for a in self.agents}
        infos = {
            a: {
                "validator_interventions": int(report.n_interventions),
                "validator_iterations": int(report.n_iterations),
                "conflicts_vertex": int(report.n_vertex),
                "conflicts_edge_swap": int(report.n_edge_swap),
                "conflicts_following": int(report.n_following),
                "tasks_completed_total": self._task_gen.num_completed(),
                "tasks_pending": self._task_gen.num_pending(),
                "tasks_in_flight": self._task_gen.num_in_flight(),
            }
            for a in self.agents
        }

        if all(truncated.values()):
            self.agents = []

        return obs, rewards, terminated, truncated, infos

    def render(self):
        return None

    def close(self):
        return None
