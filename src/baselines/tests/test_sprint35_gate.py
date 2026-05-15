"""Tests for Sprint 3.5 gate evidence runner."""

from __future__ import annotations

import importlib.util
import unittest

from src.baselines.sprint35_gate import (
    build_cbs_grid,
    run_cbs_reference,
    sample_grid_instance,
    sample_warehouse_stress_instance,
)
from src.map_parser import parse_opentcs_map
from src.baselines.benchmark import DEFAULT_MAP_FILE


class TestSprint35GateRunner(unittest.TestCase):
    def test_sample_grid_instance_is_distinct(self):
        G = build_cbs_grid(size=5)
        starts, goals = sample_grid_instance(G, num_agents=3, seed=7)

        self.assertEqual(len(starts), 3)
        self.assertEqual(len(goals), 3)
        self.assertEqual(len(set(starts.values()) | set(goals.values())), 6)

    def test_hotspot_and_burst_stress_instances_are_distinct(self):
        G = parse_opentcs_map(str(DEFAULT_MAP_FILE), restrict_to_largest_scc=True)
        for distribution in ("hotspot", "burst_wave"):
            starts, goals = sample_warehouse_stress_instance(
                G,
                num_agents=5,
                seed=3,
                distribution=distribution,
            )
            self.assertEqual(len(starts), 5)
            self.assertEqual(len(goals), 5)
            self.assertEqual(len(set(starts.values())), 5)
            self.assertEqual(len(set(goals.values())), 5)

    @unittest.skipUnless(
        importlib.util.find_spec("cbs_mapf"),
        "cbs-mapf not installed",
    )
    def test_cbs_reference_uses_external_backend_without_fallback(self):
        rows = run_cbs_reference(
            seeds=[0],
            agent_counts=[1],
            warehouse_probe=False,
            max_time=16,
            cbs_max_iter=20,
            cbs_low_level_max_iter=50,
        )

        self.assertEqual(len(rows), 1)
        self.assertTrue(rows[0].cbs_success, rows[0].cbs_diagnostics_json)
        self.assertEqual(rows[0].cbs_solver, "cbs_mapf")
        self.assertEqual(rows[0].pp32_over_cbs, 1.0)

    @unittest.skipUnless(
        importlib.util.find_spec("cbs_mapf"),
        "cbs-mapf not installed",
    )
    def test_cbs_reference_can_run_outer_jobs_in_parallel(self):
        rows = run_cbs_reference(
            seeds=[0, 1],
            agent_counts=[1],
            warehouse_probe=False,
            cbs_jobs=2,
            max_time=16,
            cbs_max_iter=20,
            cbs_low_level_max_iter=50,
        )

        self.assertEqual(len(rows), 2)
        self.assertTrue(all(row.cbs_success for row in rows))
        self.assertEqual([row.seed for row in rows], [0, 1])


if __name__ == "__main__":
    unittest.main(verbosity=2)
