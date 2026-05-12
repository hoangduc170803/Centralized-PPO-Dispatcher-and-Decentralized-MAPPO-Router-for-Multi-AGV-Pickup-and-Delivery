"""Sprint 2 classical baseline benchmark runner.

The runner creates one-shot MAPF instances on the largest connected warehouse
component, runs classical assignment/planning baselines, and writes paper-ready
CSV rows with makespan, waiting time, conflicts, success, and timing.
"""

from __future__ import annotations

import argparse
import csv
import json
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence

import networkx as nx
import numpy as np

from src.baselines.dispatchers import (
    fifo_nearest_goal_assignment,
    hungarian_goal_assignment,
)
from src.map_parser import parse_opentcs_map
from src.mapf.cbs_solver import CBSMapfPlanner, MAPFPlanResult
from src.mapf.priority_search import PrioritySearchPlanner
from src.routing.astar import AStarRouter


DEFAULT_MAP_FILE = (
    Path(__file__).resolve().parents[2]
    / "orca_share_media1778260607027_7458565577098821053.xml"
)
DEFAULT_OUTPUT = Path(__file__).resolve().parents[2] / "results" / "baselines" / "sprint2_mapf_baselines.csv"
DEFAULT_BASELINES = ("cbs", "pbs", "hungarian_cbs", "fifo_nearest")


@dataclass(frozen=True)
class BenchmarkRow:
    """One CSV row for a baseline/seed/agent-count run."""

    baseline: str
    seed: int
    num_agents: int
    success: bool
    success_rate: float
    solver: str
    instance_makespan: int
    sum_of_costs: int
    waiting_time_agent_steps: int
    throughput_tasks_per_1000_steps: float
    conflicts_total: int
    elapsed_s: float
    starts_json: str
    goals_json: str
    diagnostics_json: str


def sample_mapf_instance(
    G: nx.DiGraph,
    num_agents: int,
    seed: int,
) -> tuple[dict[str, str], dict[str, str]]:
    """Sample distinct starts and distinct direct goals for one-shot MAPF."""
    if num_agents < 1:
        raise ValueError("num_agents must be >= 1")
    nodes = np.array(list(G.nodes()), dtype=object)
    if len(nodes) < 2 * num_agents:
        raise ValueError("graph needs at least 2 * num_agents nodes")

    rng = np.random.default_rng(seed)
    chosen = rng.choice(nodes, size=2 * num_agents, replace=False)
    agents = [f"agv_{i}" for i in range(num_agents)]
    starts = {agent: str(node) for agent, node in zip(agents, chosen[:num_agents])}
    goals = {agent: str(node) for agent, node in zip(agents, chosen[num_agents:])}
    return starts, goals


def run_mapf_baseline(
    G: nx.DiGraph,
    starts: Mapping[str, str],
    goals: Mapping[str, str],
    baseline: str,
    seed: int = 0,
    max_time: int = 512,
    use_external: bool = True,
    router: AStarRouter | None = None,
) -> tuple[MAPFPlanResult, dict[str, str]]:
    """Run one Sprint 2 baseline and return result plus assigned goals."""
    baseline_key = baseline.lower()
    assigned_goals = dict(goals)

    if baseline_key in {"hungarian_cbs", "hungarian"}:
        assignment_router = router if router is not None else AStarRouter(G, precompute=True)
        assignments = hungarian_goal_assignment(starts, list(goals.values()), assignment_router)
        assigned_goals = {agent: item.goal for agent, item in assignments.items()}
        result = CBSMapfPlanner(G, max_time=max_time).plan(
            starts,
            assigned_goals,
            use_external=use_external,
        )
        result.diagnostics["assignment"] = "hungarian_goal"
        result.diagnostics["assigned_agents"] = len(assigned_goals)
        return result, assigned_goals

    if baseline_key in {"fifo_nearest", "fifo_nearest_cbs"}:
        assignment_router = router if router is not None else AStarRouter(G, precompute=True)
        assignments = fifo_nearest_goal_assignment(starts, list(goals.values()), assignment_router)
        assigned_goals = {agent: item.goal for agent, item in assignments.items()}
        result = CBSMapfPlanner(G, max_time=max_time).plan(
            starts,
            assigned_goals,
            use_external=use_external,
        )
        result.diagnostics["assignment"] = "fifo_nearest_goal"
        result.diagnostics["assigned_agents"] = len(assigned_goals)
        return result, assigned_goals

    if baseline_key in {"pbs", "priority_search"}:
        result = PrioritySearchPlanner(G, max_time=max_time).plan(
            starts,
            assigned_goals,
            seed=seed,
        )
        return result, assigned_goals

    if baseline_key in {"cbs", "direct_cbs", "cbs_mapf"}:
        result = CBSMapfPlanner(G, max_time=max_time).plan(
            starts,
            assigned_goals,
            use_external=use_external,
        )
        result.diagnostics["assignment"] = "fixed_agent_goal_pairs"
        return result, assigned_goals

    raise ValueError(f"unknown baseline: {baseline}")


def run_benchmark(
    G: nx.DiGraph,
    agent_counts: Sequence[int] = (10, 15, 20),
    seeds: Iterable[int] = range(5),
    baselines: Sequence[str] = DEFAULT_BASELINES,
    max_time: int = 512,
    use_external: bool = True,
) -> list[BenchmarkRow]:
    """Run a grid of baseline/agent-count/seed experiments."""
    rows: list[BenchmarkRow] = []
    needs_router = any(
        baseline.lower() in {"hungarian_cbs", "hungarian", "fifo_nearest", "fifo_nearest_cbs"}
        for baseline in baselines
    )
    router = AStarRouter(G, precompute=True) if needs_router else None
    for num_agents in agent_counts:
        for seed in seeds:
            starts, direct_goals = sample_mapf_instance(G, num_agents, seed)
            for baseline in baselines:
                result, assigned_goals = run_mapf_baseline(
                    G,
                    starts,
                    direct_goals,
                    baseline=baseline,
                    seed=seed,
                    max_time=max_time,
                    use_external=use_external,
                    router=router,
                )
                rows.append(
                    benchmark_row(
                        baseline=baseline,
                        seed=seed,
                        num_agents=num_agents,
                        starts=starts,
                        goals=assigned_goals,
                        result=result,
                    )
                )
    return rows


def benchmark_row(
    baseline: str,
    seed: int,
    num_agents: int,
    starts: Mapping[str, str],
    goals: Mapping[str, str],
    result: MAPFPlanResult,
) -> BenchmarkRow:
    """Convert a planner result into a stable CSV row."""
    completed = num_agents if result.success else 0
    horizon = max(result.makespan, 1)
    throughput = completed / horizon * 1000.0
    diagnostics = _json_dumpable(result.diagnostics)
    return BenchmarkRow(
        baseline=baseline,
        seed=seed,
        num_agents=num_agents,
        success=result.success,
        success_rate=1.0 if result.success else 0.0,
        solver=result.solver,
        instance_makespan=result.makespan,
        sum_of_costs=result.sum_of_costs,
        waiting_time_agent_steps=_waiting_time_agent_steps(result.paths, result.makespan),
        throughput_tasks_per_1000_steps=throughput,
        conflicts_total=len(result.conflicts),
        elapsed_s=result.elapsed_s,
        starts_json=json.dumps(dict(starts), sort_keys=True),
        goals_json=json.dumps(dict(goals), sort_keys=True),
        diagnostics_json=json.dumps(diagnostics, sort_keys=True),
    )


def write_benchmark_csv(rows: Sequence[BenchmarkRow], path: Path | str) -> Path:
    """Write benchmark rows to CSV, creating parent folders as needed."""
    output = Path(path)
    output.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = list(BenchmarkRow.__dataclass_fields__)
    with output.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(asdict(row))
    return output


def _waiting_time_agent_steps(
    paths: Mapping[str, Sequence[str]],
    makespan: int,
) -> int:
    total = 0
    for path in paths.values():
        total += sum(1 for i in range(1, len(path)) if path[i] == path[i - 1])
        total += max(0, makespan - (len(path) - 1))
    return total


def _json_dumpable(value):
    try:
        json.dumps(value)
        return value
    except TypeError:
        if isinstance(value, dict):
            return {str(k): _json_dumpable(v) for k, v in value.items()}
        if isinstance(value, (list, tuple)):
            return [_json_dumpable(v) for v in value]
        return repr(value)


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--map", type=Path, default=DEFAULT_MAP_FILE)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--agents", type=int, nargs="+", default=[10, 15, 20])
    parser.add_argument("--seeds", type=int, nargs="+", default=[0, 1, 2, 3, 4])
    parser.add_argument("--baselines", nargs="+", default=list(DEFAULT_BASELINES))
    parser.add_argument("--max-time", type=int, default=512)
    parser.add_argument(
        "--no-external",
        action="store_true",
        help="Disable optional cbs-mapf backend and use graph-native fallback.",
    )
    args = parser.parse_args(argv)

    G = parse_opentcs_map(str(args.map), restrict_to_largest_scc=True)
    rows = run_benchmark(
        G,
        agent_counts=args.agents,
        seeds=args.seeds,
        baselines=args.baselines,
        max_time=args.max_time,
        use_external=not args.no_external,
    )
    output = write_benchmark_csv(rows, args.output)
    print(f"Wrote {len(rows)} rows to {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
