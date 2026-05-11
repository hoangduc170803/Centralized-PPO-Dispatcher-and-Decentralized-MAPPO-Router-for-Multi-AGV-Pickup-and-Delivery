"""Safety validator: post-policy collision detection and resolution.

Action mask prevents single-agent invalid moves (e.g., moving to a non-neighbor),
but cannot prevent JOINT conflicts between independently-acting agents:

  - vertex conflict:   two agents move to the same cell at the same step
  - edge swap:         two agents traverse the same edge in opposite directions
  - following:         agent enters a cell another agent just vacated
                       (continuous-time / safety-distance violation)

This module runs BETWEEN the policy and `env.step`, detecting conflicts in the
joint action and forcing lower-priority agents to WAIT. Without this overlay
the thesis cannot claim collision-free behavior — the policy is stochastic and
decentralized, so coordination from policy gradient alone is not a guarantee.

Priority (higher wins):
    1. older `age` (time waited) — anti-starvation
    2. closer to goal           — tiebreaker (lets near-goal agents finish)

Logs `intervention_count` per step. A well-trained policy should drive this
metric below ~5% of total steps.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional, Sequence

import numpy as np

try:
    from .compass_mapper import WAIT_SLOT
except ImportError:
    from compass_mapper import WAIT_SLOT  # script execution


@dataclass
class ConflictReport:
    n_vertex: int = 0
    n_edge_swap: int = 0
    n_following: int = 0
    n_interventions: int = 0
    n_iterations: int = 0
    # Per-agent boolean flags from the FIRST detection pass (i.e. conflicts
    # the raw policy proposed, before any WAIT-resolution). Length matches the
    # joint action; all-False arrays of the right size are initialised by
    # `validate()` so callers can index without checking for None.
    vertex_flags: Optional[np.ndarray] = None
    edge_swap_flags: Optional[np.ndarray] = None
    following_flags: Optional[np.ndarray] = None

    @property
    def total(self) -> int:
        return self.n_vertex + self.n_edge_swap + self.n_following


class SafetyValidator:
    """Resolves joint-action conflicts by forcing low-priority agents to WAIT.

    Usage:
        validator = SafetyValidator(check_following=True)
        safe_action, report = validator.validate(
            positions, joint_action, slot_maps,
            distances_to_goal, ages,
        )
    """

    def __init__(
        self,
        check_following: bool = True,
        max_iterations: int = 16,
    ):
        self.check_following = check_following
        self.max_iterations = max_iterations

    @staticmethod
    def _next_positions(
        positions: Sequence[str],
        joint_action: np.ndarray,
        slot_maps: Sequence[dict[int, str]],
    ) -> list[str]:
        return [
            slot_maps[i].get(int(joint_action[i]), positions[i])
            for i in range(len(positions))
        ]

    def _detect_conflicts(
        self,
        positions: Sequence[str],
        joint_action: np.ndarray,
        slot_maps: Sequence[dict[int, str]],
    ) -> tuple[list[tuple[str, int, int]], ConflictReport]:
        """Return list of (conflict_type, agent_i, agent_j) and counts.

        Each unordered pair (i,j) appears at most once across the three types,
        with precedence vertex > edge_swap > following — edge swap is a strict
        subset of mutual following, and vertex covers same-target collisions.
        """
        next_pos = self._next_positions(positions, joint_action, slot_maps)
        n = len(positions)
        conflicts: list[tuple[str, int, int]] = []
        report = ConflictReport()
        handled: set[tuple[int, int]] = set()

        # Vertex: two distinct agents share a next position
        for i in range(n):
            for j in range(i + 1, n):
                if next_pos[i] == next_pos[j]:
                    conflicts.append(("vertex", i, j))
                    handled.add((i, j))
                    report.n_vertex += 1

        # Edge swap: i goes to j's current, AND j goes to i's current
        for i in range(n):
            for j in range(i + 1, n):
                if (i, j) in handled:
                    continue
                if (
                    next_pos[i] == positions[j]
                    and next_pos[j] == positions[i]
                    and positions[i] != positions[j]
                ):
                    conflicts.append(("edge_swap", i, j))
                    handled.add((i, j))
                    report.n_edge_swap += 1

        # Following: i enters j's previous cell while j moves away. Skip pairs
        # already reported as vertex / edge_swap.
        if self.check_following:
            for i in range(n):
                for j in range(n):
                    if i == j:
                        continue
                    pair = (min(i, j), max(i, j))
                    if pair in handled:
                        continue
                    j_moved = next_pos[j] != positions[j]
                    i_moved = next_pos[i] != positions[i]
                    if (
                        j_moved
                        and i_moved
                        and next_pos[i] == positions[j]
                        and next_pos[i] != next_pos[j]
                    ):
                        conflicts.append(("following", i, j))
                        handled.add(pair)
                        report.n_following += 1

        return conflicts, report

    def _conflict_still_real(
        self,
        ctype: str,
        i: int,
        j: int,
        positions: Sequence[str],
        action: np.ndarray,
        slot_maps: Sequence[dict[int, str]],
    ) -> bool:
        """Re-check whether a conflict still exists given current action."""
        ni = slot_maps[i].get(int(action[i]), positions[i])
        nj = slot_maps[j].get(int(action[j]), positions[j])
        if ctype == "vertex":
            return ni == nj
        if ctype == "edge_swap":
            return (
                ni == positions[j]
                and nj == positions[i]
                and positions[i] != positions[j]
            )
        if ctype == "following":
            j_moved = nj != positions[j]
            i_moved = ni != positions[i]
            return j_moved and i_moved and ni == positions[j] and ni != nj
        return False

    @staticmethod
    def _priority(age: float, dist_to_goal: float) -> tuple[float, float]:
        """Higher tuple wins. Older age first, then closer to goal."""
        return (age, -dist_to_goal)

    def _pick_loser(
        self,
        i: int,
        j: int,
        ages: np.ndarray,
        distances_to_goal: np.ndarray,
    ) -> int:
        pi = self._priority(float(ages[i]), float(distances_to_goal[i]))
        pj = self._priority(float(ages[j]), float(distances_to_goal[j]))
        return j if pi >= pj else i

    def validate(
        self,
        positions: Sequence[str],
        joint_action: np.ndarray,
        slot_maps: Sequence[dict[int, str]],
        distances_to_goal: np.ndarray,
        ages: np.ndarray,
    ) -> tuple[np.ndarray, ConflictReport]:
        """Return (safe_joint_action, report). Original arrays not mutated.

        Conflict counts and per-agent flags in the report are from the FIRST
        detection pass before any resolution, so they reflect what the raw
        policy proposed. Per-agent flags let downstream reward shaping credit
        the agents that contributed to a raw conflict, even though the
        validator scrubs the action before env.step. The intervention count
        is the number of agents forced to WAIT in total.
        """
        action = np.array(joint_action, dtype=np.int64, copy=True)
        n_agents = len(positions)
        cumulative = ConflictReport(
            vertex_flags=np.zeros(n_agents, dtype=bool),
            edge_swap_flags=np.zeros(n_agents, dtype=bool),
            following_flags=np.zeros(n_agents, dtype=bool),
        )
        first_pass = True

        for it in range(self.max_iterations):
            conflicts, report = self._detect_conflicts(positions, action, slot_maps)
            if first_pass:
                cumulative.n_vertex = report.n_vertex
                cumulative.n_edge_swap = report.n_edge_swap
                cumulative.n_following = report.n_following
                for ctype, i, j in conflicts:
                    if ctype == "vertex":
                        cumulative.vertex_flags[i] = True
                        cumulative.vertex_flags[j] = True
                    elif ctype == "edge_swap":
                        cumulative.edge_swap_flags[i] = True
                        cumulative.edge_swap_flags[j] = True
                    elif ctype == "following":
                        cumulative.following_flags[i] = True
                        cumulative.following_flags[j] = True
                first_pass = False

            if not conflicts:
                cumulative.n_iterations = it
                return action, cumulative

            progress = False
            for ctype, i, j in conflicts:
                # Skip conflicts that earlier resolutions in this iter invalidated.
                if not self._conflict_still_real(
                    ctype, i, j, positions, action, slot_maps
                ):
                    continue
                if action[i] == WAIT_SLOT and action[j] == WAIT_SLOT:
                    continue
                loser = self._pick_loser(i, j, ages, distances_to_goal)
                if action[loser] == WAIT_SLOT:
                    loser = i if loser == j else j
                if action[loser] != WAIT_SLOT:
                    action[loser] = WAIT_SLOT
                    cumulative.n_interventions += 1
                    progress = True

            if not progress:
                break

        cumulative.n_iterations = self.max_iterations
        return action, cumulative


if __name__ == "__main__":
    import sys
    from pathlib import Path

    repo_root = Path(__file__).resolve().parents[2]
    sys.path.insert(0, str(repo_root / "src"))
    from map_parser import parse_opentcs_map
    from env.compass_mapper import CompassMapper

    G = parse_opentcs_map(
        str(repo_root / "orca_share_media1778260607027_7458565577098821053.xml"),
        restrict_to_largest_scc=True,
    )
    mapper = CompassMapper(G)

    print("Smoke test — set up 3 agents with deliberate conflicts.")
    # Pick a node with multiple in-edges so we can engineer vertex conflict.
    target_node = None
    for n in G.nodes():
        if G.in_degree(n) >= 2:
            target_node = n
            break
    if target_node is None:
        print("No node with 2+ predecessors found, abort.")
        sys.exit(0)

    preds = list(G.predecessors(target_node))[:2]
    print(f"Target: {target_node}  preds: {preds}")

    positions = [preds[0], preds[1], list(G.nodes())[0]]
    slot_maps = [mapper.get(p)[1] for p in positions]

    # Find slots that move into target_node from each pred
    def slot_for_target(slot_map, target):
        for s, n in slot_map.items():
            if n == target:
                return s
        return WAIT_SLOT

    action = np.array(
        [
            slot_for_target(slot_maps[0], target_node),
            slot_for_target(slot_maps[1], target_node),
            WAIT_SLOT,
        ]
    )

    print(f"Raw action: {action.tolist()}")
    validator = SafetyValidator()
    safe, report = validator.validate(
        positions=positions,
        joint_action=action,
        slot_maps=slot_maps,
        distances_to_goal=np.array([100.0, 200.0, 0.0]),
        ages=np.array([5.0, 1.0, 0.0]),
    )
    print(f"Safe action: {safe.tolist()}")
    print(f"Report: vertex={report.n_vertex} edge_swap={report.n_edge_swap} "
          f"following={report.n_following} interventions={report.n_interventions} "
          f"iters={report.n_iterations}")
    print("Expected: agent with lower priority (age=1, idx=1) forced to WAIT.")
