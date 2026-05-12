"""Tests for benchmark CSV aggregation."""

from __future__ import annotations

import math
import unittest

from src.baselines.aggregate import aggregate_rows, render_markdown


class TestAggregate(unittest.TestCase):
    def test_groups_rows_and_counts_failure_reasons(self):
        summary = aggregate_rows(
            [
                {
                    "baseline": "priority_search",
                    "num_agents": "10",
                    "success": "True",
                    "instance_makespan": "50",
                    "makespan_over_lower_bound": "1.25",
                    "waiting_time_agent_steps": "20",
                    "elapsed_s": "0.1",
                    "failure_reason": "",
                },
                {
                    "baseline": "priority_search",
                    "num_agents": "10",
                    "success": "False",
                    "instance_makespan": "0",
                    "makespan_over_lower_bound": "inf",
                    "waiting_time_agent_steps": "0",
                    "elapsed_s": "0.2",
                    "failure_reason": "timeout",
                },
            ]
        )
        self.assertEqual(len(summary), 1)
        row = summary[0]
        self.assertEqual(row["runs"], 2)
        self.assertAlmostEqual(row["success_rate"], 0.5)
        self.assertAlmostEqual(row["mean_makespan_success"], 50.0)
        self.assertTrue(math.isnan(row["std_makespan_success"]))
        self.assertEqual(row["failure_reasons"], "timeout:1")
        self.assertIn("priority_search", render_markdown(summary))


if __name__ == "__main__":
    unittest.main(verbosity=2)
