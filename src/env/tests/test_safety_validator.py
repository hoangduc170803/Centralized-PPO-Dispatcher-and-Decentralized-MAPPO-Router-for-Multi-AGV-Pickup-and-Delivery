"""Unit tests for safety_validator on synthetic graphs.

Run from `src/` directory:
    python -m unittest env.tests.test_safety_validator
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

import networkx as nx
import numpy as np

# Allow `python env/tests/test_safety_validator.py` from src/
sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from env.compass_mapper import WAIT_SLOT, CompassMapper, compute_action_mask
from env.safety_validator import SafetyValidator


def _grid_graph(rows: int, cols: int, spacing: float = 1.0) -> nx.DiGraph:
    """Build a 4-connected grid as a directed graph for testing."""
    G = nx.DiGraph()
    for r in range(rows):
        for c in range(cols):
            G.add_node(f"{r},{c}", x=c * spacing, y=-r * spacing)
    for r in range(rows):
        for c in range(cols):
            here = f"{r},{c}"
            for dr, dc in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                nr, nc = r + dr, c + dc
                if 0 <= nr < rows and 0 <= nc < cols:
                    G.add_edge(here, f"{nr},{nc}", weight=spacing)
    return G


class TestCompassMapper(unittest.TestCase):
    def setUp(self):
        self.G = _grid_graph(3, 3)
        self.mapper = CompassMapper(self.G)

    def test_corner_node_has_two_directions_plus_wait(self):
        # top-left "0,0": only down and right are valid, plus wait
        mask, slot_to_nbr = self.mapper.get("0,0")
        self.assertEqual(int(mask.sum()), 3)
        self.assertEqual(mask[WAIT_SLOT], 1)
        self.assertIn("1,0", slot_to_nbr.values())  # down
        self.assertIn("0,1", slot_to_nbr.values())  # right

    def test_center_node_has_four_directions_plus_wait(self):
        mask, _ = self.mapper.get("1,1")
        self.assertEqual(int(mask.sum()), 5)

    def test_wait_slot_always_valid(self):
        for node in self.G.nodes():
            mask, slot_to_nbr = self.mapper.get(node)
            self.assertEqual(mask[WAIT_SLOT], 1)
            self.assertEqual(slot_to_nbr[WAIT_SLOT], node)


class TestSafetyValidator(unittest.TestCase):
    def setUp(self):
        self.G = _grid_graph(3, 3)
        self.mapper = CompassMapper(self.G)
        self.validator = SafetyValidator()

    def _act(self, pos: str, target: str) -> int:
        _, s2n = self.mapper.get(pos)
        for slot, nbr in s2n.items():
            if nbr == target:
                return slot
        return WAIT_SLOT

    def test_no_conflict_passes_through(self):
        positions = ["0,0", "2,2"]
        slot_maps = [self.mapper.get(p)[1] for p in positions]
        action = np.array([self._act("0,0", "0,1"), self._act("2,2", "2,1")])
        safe, report = self.validator.validate(
            positions, action, slot_maps,
            distances_to_goal=np.array([1.0, 1.0]),
            ages=np.array([0.0, 0.0]),
        )
        self.assertEqual(safe.tolist(), action.tolist())
        self.assertEqual(report.n_interventions, 0)
        self.assertEqual(report.total, 0)

    def test_vertex_conflict_forces_lower_priority_to_wait(self):
        # Agents at (0,1) and (1,0) both move to (1,1)
        positions = ["0,1", "1,0"]
        slot_maps = [self.mapper.get(p)[1] for p in positions]
        action = np.array(
            [self._act("0,1", "1,1"), self._act("1,0", "1,1")]
        )
        # Agent 0 has higher age → priority
        safe, report = self.validator.validate(
            positions, action, slot_maps,
            distances_to_goal=np.array([5.0, 5.0]),
            ages=np.array([10.0, 0.0]),
        )
        self.assertEqual(report.n_vertex, 1)
        self.assertEqual(report.n_interventions, 1)
        self.assertEqual(int(safe[0]), int(action[0]))  # winner keeps action
        self.assertEqual(int(safe[1]), WAIT_SLOT)        # loser waits

    def test_edge_swap_conflict_resolved(self):
        # Agent 0 at (1,0), Agent 1 at (1,1). Each moves to the other's cell.
        # An edge swap cannot be resolved by waiting one agent only — the
        # other would then collide with the waiter at the now-occupied target.
        # The conservative resolution forces BOTH agents to wait this step.
        positions = ["1,0", "1,1"]
        slot_maps = [self.mapper.get(p)[1] for p in positions]
        action = np.array(
            [self._act("1,0", "1,1"), self._act("1,1", "1,0")]
        )
        safe, report = self.validator.validate(
            positions, action, slot_maps,
            distances_to_goal=np.array([1.0, 1.0]),
            ages=np.array([3.0, 1.0]),
        )
        self.assertEqual(report.n_edge_swap, 1)
        self.assertEqual(report.n_interventions, 2)
        self.assertEqual(int(safe[0]), WAIT_SLOT)
        self.assertEqual(int(safe[1]), WAIT_SLOT)

    def test_three_way_vertex_conflict_resolves(self):
        # Three agents at (0,1), (1,0), (2,1) all want to move to (1,1)
        positions = ["0,1", "1,0", "2,1"]
        slot_maps = [self.mapper.get(p)[1] for p in positions]
        action = np.array(
            [
                self._act("0,1", "1,1"),
                self._act("1,0", "1,1"),
                self._act("2,1", "1,1"),
            ]
        )
        safe, report = self.validator.validate(
            positions, action, slot_maps,
            distances_to_goal=np.array([10.0, 5.0, 1.0]),
            ages=np.array([7.0, 7.0, 7.0]),  # same age, distance breaks tie
        )
        # Closest to goal (idx 2) should win since age ties.
        self.assertEqual(int(safe[2]), int(action[2]))
        # Other two forced to wait.
        self.assertEqual(int(safe[0]), WAIT_SLOT)
        self.assertEqual(int(safe[1]), WAIT_SLOT)
        self.assertEqual(report.n_interventions, 2)

    def test_following_conflict_detected(self):
        # Agent 0 at (1,1) moves to (1,2). Agent 1 at (1,0) moves to (1,1).
        # Agent 1 is following into where Agent 0 just vacated.
        positions = ["1,1", "1,0"]
        slot_maps = [self.mapper.get(p)[1] for p in positions]
        action = np.array(
            [self._act("1,1", "1,2"), self._act("1,0", "1,1")]
        )
        safe, report = self.validator.validate(
            positions, action, slot_maps,
            distances_to_goal=np.array([1.0, 1.0]),
            ages=np.array([0.0, 5.0]),
        )
        self.assertGreaterEqual(report.n_following, 1)

    def test_following_disabled_keeps_action(self):
        positions = ["1,1", "1,0"]
        slot_maps = [self.mapper.get(p)[1] for p in positions]
        action = np.array(
            [self._act("1,1", "1,2"), self._act("1,0", "1,1")]
        )
        v = SafetyValidator(check_following=False)
        safe, report = v.validate(
            positions, action, slot_maps,
            distances_to_goal=np.array([1.0, 1.0]),
            ages=np.array([0.0, 5.0]),
        )
        self.assertEqual(report.n_following, 0)
        self.assertEqual(safe.tolist(), action.tolist())

    def test_validator_does_not_mutate_input_action(self):
        positions = ["0,1", "1,0"]
        slot_maps = [self.mapper.get(p)[1] for p in positions]
        action = np.array(
            [self._act("0,1", "1,1"), self._act("1,0", "1,1")]
        )
        original = action.copy()
        self.validator.validate(
            positions, action, slot_maps,
            distances_to_goal=np.array([1.0, 1.0]),
            ages=np.array([5.0, 0.0]),
        )
        self.assertTrue(np.array_equal(action, original))


if __name__ == "__main__":
    unittest.main(verbosity=2)
