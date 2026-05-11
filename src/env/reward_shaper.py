"""Per-step reward computation for Layer 2 Router agents.

Components follow PLAN.md §1 "Reward design". All weights live in `RewardConfig`
so tuning is one config change away. Progress shaping uses Δ(A*_dist) which is
an approximate potential-based shaping (Ng et al. 1999) with γ ≈ 1.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional


@dataclass
class RewardConfig:
    r_step: float = -0.01
    r_goal: float = 1.0                       # completing dropoff
    r_pickup: float = 0.2                     # smaller bonus for the pickup leg
    r_vertex_collision: float = -1.0          # raw policy bug (validator catches)
    r_edge_swap: float = -0.2
    r_following: float = -0.1
    r_invalid_action: float = -0.1            # mask should prevent this
    r_unnecessary_wait: float = -0.05
    r_forced_wait_by_validator: float = -0.5
    r_progress_coef: float = 0.02             # × Δ(A*_dist) / dist_normalizer
    # Divisor applied to Δ(A*_dist) so the shaping term lives on the same scale
    # regardless of raw edge weights. Set by WarehouseEnv to the max all-pairs
    # A* distance (or bbox diagonal fallback). Defaults to 1.0 when unknown so
    # standalone use of RewardConfig stays backward-compatible.
    dist_normalizer: float = 1.0


@dataclass
class AgentRewardSignal:
    """Per-agent flags consumed by the reward shaper each step."""

    reached_pickup: bool = False
    reached_dropoff: bool = False
    raw_action_was_wait: bool = False
    had_valid_move: bool = False              # other than WAIT in action_mask
    forced_to_wait: bool = False              # safety validator override
    invalid_action_requested: bool = False    # mask was 0 for chosen slot
    in_vertex_conflict: bool = False
    in_edge_swap: bool = False
    in_following_conflict: bool = False
    dist_to_goal_prev: float = 0.0
    dist_to_goal_now: float = 0.0
    has_active_task: bool = True


def compute_reward(signal: AgentRewardSignal, cfg: RewardConfig) -> float:
    """Return scalar reward for one agent given its signal this step."""
    r = cfg.r_step

    if signal.reached_dropoff:
        r += cfg.r_goal
    if signal.reached_pickup:
        r += cfg.r_pickup

    if signal.has_active_task:
        # Δ = dist_prev - dist_now; positive when getting closer.
        # Divide by dist_normalizer so a one-step move contributes O(1/diam),
        # keeping per-step shaping comparable to r_step / r_goal regardless of
        # the map's raw edge weights.
        denom = cfg.dist_normalizer if cfg.dist_normalizer > 1e-9 else 1.0
        delta = (signal.dist_to_goal_prev - signal.dist_to_goal_now) / denom
        r += cfg.r_progress_coef * delta

    if signal.in_vertex_conflict:
        r += cfg.r_vertex_collision
    if signal.in_edge_swap:
        r += cfg.r_edge_swap
    if signal.in_following_conflict:
        r += cfg.r_following

    if signal.invalid_action_requested:
        r += cfg.r_invalid_action
    if signal.forced_to_wait:
        r += cfg.r_forced_wait_by_validator
    if signal.raw_action_was_wait and signal.had_valid_move and not signal.forced_to_wait:
        r += cfg.r_unnecessary_wait

    return r
