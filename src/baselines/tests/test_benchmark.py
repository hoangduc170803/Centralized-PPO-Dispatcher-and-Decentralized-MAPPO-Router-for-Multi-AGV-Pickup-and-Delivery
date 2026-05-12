"""Tests for Sprint 2 benchmark CSV runner."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

import networkx as nx

from src.baselines.benchmark import (
    benchmark_row,
    run_benchmark,
    run_mapf_baseline,
    sample_mapf_instance,
    write_benchmark_csv,
)


def _grid_graph(rows: int, cols: int) -> nx.DiGraph:
    G = nx.DiGraph()
    for r in range(rows):
        for c in range(cols):
            G.add_node(f"{r},{c}", x=float(c), y=float(r), is_halt=False, type="POINT")
    for r in range(rows):
        for c in range(cols):
            here = f"{r},{c}"
            for dr, dc in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                nr, nc = r + dr, c + dc
                if 0 <= nr < rows and 0 <= nc < cols:
                    G.add_edge(here, f"{nr},{nc}", weight=1.0, length=1.0)
    return G


class TestBenchmarkRunner(unittest.TestCase):
    def test_sample_mapf_instance_is_deterministic_and_distinct(self):
        G = _grid_graph(4, 4)
        starts_a, goals_a = sample_mapf_instance(G, num_agents=3, seed=42)
        starts_b, goals_b = sample_mapf_instance(G, num_agents=3, seed=42)
        self.assertEqual(starts_a, starts_b)
        self.assertEqual(goals_a, goals_b)
        self.assertEqual(len(set(starts_a.values()) | set(goals_a.values())), 6)

    def test_runs_pbs_and_hungarian_cbs_rows(self):
        G = _grid_graph(4, 4)
        starts = {"a0": "0,0", "a1": "3,3"}
        goals = {"a0": "3,3", "a1": "0,0"}

        pbs_result, pbs_goals = run_mapf_baseline(
            G,
            starts,
            goals,
            baseline="pbs",
            max_time=16,
            use_external=False,
        )
        self.assertTrue(pbs_result.success, pbs_result.diagnostics)
        pbs_row = benchmark_row("pbs", 0, 2, starts, pbs_goals, pbs_result)
        self.assertEqual(pbs_row.success_rate, 1.0)
        self.assertEqual(pbs_row.conflicts_total, 0)

        cbs_result, cbs_goals = run_mapf_baseline(
            G,
            starts,
            goals,
            baseline="hungarian_cbs",
            max_time=16,
            use_external=False,
        )
        self.assertTrue(cbs_result.success, cbs_result.diagnostics)
        self.assertEqual(set(cbs_goals), set(starts))

    def test_run_benchmark_writes_csv(self):
        G = _grid_graph(4, 4)
        rows = run_benchmark(
            G,
            agent_counts=[2],
            seeds=[0],
            baselines=["pbs", "fifo_nearest"],
            max_time=16,
            use_external=False,
        )
        self.assertEqual(len(rows), 2)
        with tempfile.TemporaryDirectory() as tmp:
            out = write_benchmark_csv(rows, Path(tmp) / "baselines.csv")
            self.assertTrue(out.exists())
            self.assertIn("instance_makespan", out.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
