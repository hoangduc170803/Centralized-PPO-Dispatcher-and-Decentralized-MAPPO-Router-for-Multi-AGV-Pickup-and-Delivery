"""Sprint 3.5 gate evidence runner.

M1.1 uses the optional `cbs-mapf` backend directly, with fallback disabled, so
rows marked as CBS references are only counted when `solver == "cbs_mapf"`.
M1.2 stresses the existing graph-native warehouse baselines on harder one-shot
MAPF distributions: clustered hotspot goals and burst-wave cross-warehouse flow.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import csv
import importlib.metadata
import json
import math
import statistics
import time
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence

import networkx as nx
import numpy as np

from src.baselines.benchmark import (
    DEFAULT_BASELINES,
    DEFAULT_MAP_FILE,
    BaselineRun,
    benchmark_row,
    run_mapf_baseline,
)
from src.baselines.dispatchers import hungarian_goal_assignment
from src.map_parser import parse_opentcs_map
from src.mapf.cbs_solver import CBSMapfPlanner, MAPFPlanResult
from src.mapf.priority_search import PrioritySearchPlanner
from src.routing.astar import AStarRouter


DEFAULT_OUTPUT_DIR = (
    Path(__file__).resolve().parents[2] / "results" / "gate"
)
DEFAULT_CBS_COUNTS = (5,)
DEFAULT_STRESS_COUNTS = (10, 15, 20)
DEFAULT_SEEDS = tuple(range(10))
STRESS_DISTRIBUTIONS = ("hotspot", "burst_wave")
_STRESS_G: nx.DiGraph | None = None
_STRESS_ROUTER: AStarRouter | None = None
_CBS_GRID_CACHE: dict[int, tuple[nx.DiGraph, AStarRouter]] = {}
_CBS_WAREHOUSE_CACHE: tuple[nx.DiGraph, AStarRouter] | None = None


@dataclass(frozen=True)
class CBSReferenceRow:
    scenario: str
    distribution: str
    seed: int
    num_agents: int
    cbs_mapf_version: str
    cbs_success: bool
    cbs_solver: str
    cbs_failure_reason: str
    cbs_makespan: int
    cbs_sum_of_costs: int
    cbs_elapsed_s: float
    lower_bound_steps: int
    cbs_over_lower_bound: float
    pp32_success: bool
    pp32_makespan: int
    pp32_over_cbs: float
    pp32_over_lower_bound: float
    pp32_elapsed_s: float
    pp32_orders_tried: int
    default_pp_success: bool
    default_pp_makespan: int
    default_pp_over_cbs: float
    hungarian_cbs_success: bool
    hungarian_cbs_makespan: int
    hungarian_cbs_failure_reason: str
    hungarian_pp_success: bool
    hungarian_pp_makespan: int
    hungarian_pp_over_hungarian_cbs: float
    starts_json: str
    goals_json: str
    cbs_diagnostics_json: str


def build_cbs_grid(size: int = 8) -> nx.DiGraph:
    """Return an 8-neighbor directed grid compatible with cbs-mapf."""
    G = nx.DiGraph()
    directions = (
        (-1, 0),
        (1, 0),
        (0, -1),
        (0, 1),
        (-1, -1),
        (-1, 1),
        (1, -1),
        (1, 1),
    )
    for row in range(size):
        for col in range(size):
            G.add_node(f"{row},{col}", x=float(col), y=float(row))
    for row in range(size):
        for col in range(size):
            here = f"{row},{col}"
            for dr, dc in directions:
                nr = row + dr
                nc = col + dc
                if 0 <= nr < size and 0 <= nc < size:
                    G.add_edge(here, f"{nr},{nc}", length=1.0, weight=1.0)
    return G


def sample_grid_instance(
    G: nx.DiGraph,
    num_agents: int,
    seed: int,
) -> tuple[dict[str, str], dict[str, str]]:
    rng = np.random.default_rng(seed)
    nodes = np.array(list(G.nodes()), dtype=object)
    chosen = rng.choice(nodes, size=2 * num_agents, replace=False)
    agents = [f"agv_{idx}" for idx in range(num_agents)]
    starts = {agent: str(node) for agent, node in zip(agents, chosen[:num_agents])}
    goals = {agent: str(node) for agent, node in zip(agents, chosen[num_agents:])}
    return starts, goals


def sample_warehouse_stress_instance(
    G: nx.DiGraph,
    num_agents: int,
    seed: int,
    distribution: str,
) -> tuple[dict[str, str], dict[str, str]]:
    """Sample harder one-shot MAPF instances on the warehouse graph."""
    rng = np.random.default_rng(seed)
    nodes = np.array(list(G.nodes()), dtype=object)
    agents = [f"agv_{idx}" for idx in range(num_agents)]
    if distribution == "hotspot":
        starts = rng.choice(nodes, size=num_agents, replace=False)
        goal_pool = _central_hotspot_pool(G, min_size=max(2 * num_agents, 24))
        goals = rng.choice(np.array(goal_pool, dtype=object), size=num_agents, replace=False)
    elif distribution == "burst_wave":
        start_pool, goal_pool = _opposite_side_pools(G, min_size=max(2 * num_agents, 40))
        starts = rng.choice(np.array(start_pool, dtype=object), size=num_agents, replace=False)
        goals = rng.choice(np.array(goal_pool, dtype=object), size=num_agents, replace=False)
    else:
        raise ValueError(f"unknown stress distribution: {distribution}")
    return (
        {agent: str(node) for agent, node in zip(agents, starts)},
        {agent: str(node) for agent, node in zip(agents, goals)},
    )


def run_cbs_reference(
    seeds: Iterable[int] = DEFAULT_SEEDS,
    agent_counts: Sequence[int] = DEFAULT_CBS_COUNTS,
    grid_size: int = 8,
    warehouse_probe: bool = True,
    cbs_max_iter: int = 200,
    cbs_low_level_max_iter: int = 300,
    cbs_max_process: int = 1,
    cbs_jobs: int = 1,
    max_time: int = 128,
) -> list[CBSReferenceRow]:
    if cbs_jobs > 1:
        return _run_cbs_reference_parallel(
            seeds=tuple(seeds),
            agent_counts=agent_counts,
            grid_size=grid_size,
            warehouse_probe=warehouse_probe,
            cbs_max_iter=cbs_max_iter,
            cbs_low_level_max_iter=cbs_low_level_max_iter,
            cbs_max_process=cbs_max_process,
            max_time=max_time,
            cbs_jobs=cbs_jobs,
        )

    rows: list[CBSReferenceRow] = []
    version = importlib.metadata.version("cbs-mapf")
    grid_graph = build_cbs_grid(grid_size)
    rows.extend(
        _run_cbs_reference_on_graph(
            G=grid_graph,
            scenario="cbs_grid_reference",
            distribution="uniform_grid",
            seeds=seeds,
            agent_counts=agent_counts,
            cbs_mapf_version=version,
            cbs_max_iter=cbs_max_iter,
            cbs_low_level_max_iter=cbs_low_level_max_iter,
            cbs_max_process=cbs_max_process,
            max_time=max_time,
        )
    )
    if warehouse_probe:
        warehouse = parse_opentcs_map(str(DEFAULT_MAP_FILE), restrict_to_largest_scc=True)
        rows.extend(
            _run_cbs_reference_on_graph(
                G=warehouse,
                scenario="warehouse_external_cbs_probe",
                distribution="uniform_warehouse",
                seeds=seeds,
                agent_counts=agent_counts,
                cbs_mapf_version=version,
                cbs_max_iter=100,
                cbs_low_level_max_iter=100,
                cbs_max_process=cbs_max_process,
                max_time=max_time,
            )
        )
    return rows


def _run_cbs_reference_parallel(
    seeds: Sequence[int],
    agent_counts: Sequence[int],
    grid_size: int,
    warehouse_probe: bool,
    cbs_max_iter: int,
    cbs_low_level_max_iter: int,
    cbs_max_process: int,
    max_time: int,
    cbs_jobs: int,
) -> list[CBSReferenceRow]:
    version = importlib.metadata.version("cbs-mapf")
    jobs: list[tuple[str, str, int, int, int, str, int, int, int, int]] = []
    for num_agents in agent_counts:
        for seed in seeds:
            jobs.append(
                (
                    "cbs_grid_reference",
                    "uniform_grid",
                    grid_size,
                    num_agents,
                    seed,
                    version,
                    cbs_max_iter,
                    cbs_low_level_max_iter,
                    cbs_max_process,
                    max_time,
                )
            )
            if warehouse_probe:
                jobs.append(
                    (
                        "warehouse_external_cbs_probe",
                        "uniform_warehouse",
                        grid_size,
                        num_agents,
                        seed,
                        version,
                        100,
                        100,
                        cbs_max_process,
                        max_time,
                    )
                )

    rows: list[CBSReferenceRow] = []
    max_workers = max(1, min(cbs_jobs, len(jobs)))
    with concurrent.futures.ProcessPoolExecutor(max_workers=max_workers) as executor:
        future_to_job = {
            executor.submit(_run_cbs_reference_job, job): job
            for job in jobs
        }
        completed = 0
        total = len(future_to_job)
        for future in concurrent.futures.as_completed(future_to_job):
            row = future.result()
            rows.append(row)
            completed += 1
            print(
                f"[cbs] {completed}/{total} {row.scenario} "
                f"N={row.num_agents} seed={row.seed} success={row.cbs_success}",
                flush=True,
            )
    rows.sort(key=lambda row: (row.scenario, row.num_agents, row.seed))
    return rows


def _run_cbs_reference_job(
    job: tuple[str, str, int, int, int, str, int, int, int, int],
) -> CBSReferenceRow:
    (
        scenario,
        distribution,
        grid_size,
        num_agents,
        seed,
        version,
        cbs_max_iter,
        cbs_low_level_max_iter,
        cbs_max_process,
        max_time,
    ) = job
    G, router = _cbs_graph_and_router(scenario, grid_size)
    starts, goals = (
        sample_grid_instance(G, num_agents, seed)
        if scenario == "cbs_grid_reference"
        else _sample_distinct_nodes(G, num_agents, seed)
    )
    cbs = CBSMapfPlanner(
        G,
        max_time=max_time,
        max_iter=cbs_max_iter,
        low_level_max_iter=cbs_low_level_max_iter,
        max_process=cbs_max_process,
    ).plan(starts, goals, use_external=True, fallback_on_failure=False)
    pp32 = PrioritySearchPlanner(G, max_time=max_time, max_orders=32).plan(
        starts,
        goals,
        seed=seed,
    )
    default_pp = CBSMapfPlanner(G, max_time=max_time).plan(
        starts,
        goals,
        use_external=False,
    )
    hungarian_run = _hungarian_pp(G, starts, goals, router, max_time)
    hungarian_cbs = CBSMapfPlanner(
        G,
        max_time=max_time,
        max_iter=cbs_max_iter,
        low_level_max_iter=cbs_low_level_max_iter,
        max_process=cbs_max_process,
    ).plan(
        starts,
        hungarian_run.goals,
        use_external=True,
        fallback_on_failure=False,
    )
    return _cbs_reference_row(
        scenario=scenario,
        distribution=distribution,
        seed=seed,
        num_agents=num_agents,
        version=version,
        starts=starts,
        goals=goals,
        cbs=cbs,
        pp32=pp32,
        default_pp=default_pp,
        hungarian_cbs=hungarian_cbs,
        hungarian_pp=hungarian_run.result,
        lower_bound_steps=_lower_bound_steps(starts, goals, router),
        pp32_elapsed_s=pp32.elapsed_s,
        hungarian_goals=hungarian_run.goals,
    )


def _cbs_graph_and_router(
    scenario: str,
    grid_size: int,
) -> tuple[nx.DiGraph, AStarRouter]:
    global _CBS_WAREHOUSE_CACHE
    if scenario == "cbs_grid_reference":
        cached = _CBS_GRID_CACHE.get(grid_size)
        if cached is None:
            G = build_cbs_grid(grid_size)
            cached = (G, AStarRouter(G, precompute=True))
            _CBS_GRID_CACHE[grid_size] = cached
        return cached
    if _CBS_WAREHOUSE_CACHE is None:
        G = parse_opentcs_map(str(DEFAULT_MAP_FILE), restrict_to_largest_scc=True)
        _CBS_WAREHOUSE_CACHE = (G, AStarRouter(G, precompute=True))
    return _CBS_WAREHOUSE_CACHE


def _run_cbs_reference_on_graph(
    G: nx.DiGraph,
    scenario: str,
    distribution: str,
    seeds: Iterable[int],
    agent_counts: Sequence[int],
    cbs_mapf_version: str,
    cbs_max_iter: int,
    cbs_low_level_max_iter: int,
    cbs_max_process: int,
    max_time: int,
) -> list[CBSReferenceRow]:
    rows: list[CBSReferenceRow] = []
    router = AStarRouter(G, precompute=True)
    for num_agents in agent_counts:
        for seed in seeds:
            starts, goals = (
                sample_grid_instance(G, num_agents, seed)
                if scenario == "cbs_grid_reference"
                else _sample_distinct_nodes(G, num_agents, seed)
            )
            cbs = CBSMapfPlanner(
                G,
                max_time=max_time,
                max_iter=cbs_max_iter,
                low_level_max_iter=cbs_low_level_max_iter,
                max_process=cbs_max_process,
            ).plan(starts, goals, use_external=True, fallback_on_failure=False)
            pp32 = PrioritySearchPlanner(G, max_time=max_time, max_orders=32).plan(
                starts,
                goals,
                seed=seed,
            )
            default_pp = CBSMapfPlanner(G, max_time=max_time).plan(
                starts,
                goals,
                use_external=False,
            )
            hungarian_run = _hungarian_pp(G, starts, goals, router, max_time)
            hungarian_cbs = CBSMapfPlanner(
                G,
                max_time=max_time,
                max_iter=cbs_max_iter,
                low_level_max_iter=cbs_low_level_max_iter,
                max_process=cbs_max_process,
            ).plan(
                starts,
                hungarian_run.goals,
                use_external=True,
                fallback_on_failure=False,
            )
            lower_bound = _lower_bound_steps(starts, goals, router)
            rows.append(
                _cbs_reference_row(
                    scenario=scenario,
                    distribution=distribution,
                    seed=seed,
                    num_agents=num_agents,
                    version=cbs_mapf_version,
                    starts=starts,
                    goals=goals,
                    cbs=cbs,
                    pp32=pp32,
                    default_pp=default_pp,
                    hungarian_cbs=hungarian_cbs,
                    hungarian_pp=hungarian_run.result,
                    lower_bound_steps=lower_bound,
                    pp32_elapsed_s=pp32.elapsed_s,
                    hungarian_goals=hungarian_run.goals,
                )
            )
    return rows


def run_stress(
    seeds: Iterable[int] = DEFAULT_SEEDS,
    agent_counts: Sequence[int] = DEFAULT_STRESS_COUNTS,
    distributions: Sequence[str] = STRESS_DISTRIBUTIONS,
    baselines: Sequence[str] = DEFAULT_BASELINES,
    max_time: int = 512,
    jobs: int = 1,
) -> list[dict[str, object]]:
    jobspecs = [
        (distribution, num_agents, seed, tuple(baselines), max_time)
        for distribution in distributions
        for num_agents in agent_counts
        for seed in seeds
    ]
    if jobs > 1:
        return _run_stress_parallel(jobspecs, jobs)

    G = parse_opentcs_map(str(DEFAULT_MAP_FILE), restrict_to_largest_scc=True)
    router = AStarRouter(G, precompute=True)
    rows: list[dict[str, object]] = []
    for job in jobspecs:
        rows.extend(_run_stress_job_on_graph(G, router, job))
    return rows


def _run_stress_parallel(
    jobspecs: Sequence[tuple[str, int, int, tuple[str, ...], int]],
    jobs: int,
) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    max_workers = max(1, min(jobs, len(jobspecs)))
    with concurrent.futures.ProcessPoolExecutor(
        max_workers=max_workers,
        initializer=_init_stress_worker,
        initargs=(str(DEFAULT_MAP_FILE),),
    ) as executor:
        future_to_job = {
            executor.submit(_run_stress_job, job): job
            for job in jobspecs
        }
        completed = 0
        total = len(future_to_job)
        for future in concurrent.futures.as_completed(future_to_job):
            job = future_to_job[future]
            rows.extend(future.result())
            completed += 1
            distribution, num_agents, seed, _, _ = job
            print(
                f"[stress] {completed}/{total} {distribution} "
                f"N={num_agents} seed={seed}",
                flush=True,
            )
    rows.sort(
        key=lambda row: (
            str(row["distribution"]),
            int(row["num_agents"]),
            int(row["seed"]),
            str(row["baseline"]),
        )
    )
    return rows


def _init_stress_worker(map_file: str) -> None:
    global _STRESS_G, _STRESS_ROUTER
    _STRESS_G = parse_opentcs_map(map_file, restrict_to_largest_scc=True)
    _STRESS_ROUTER = AStarRouter(_STRESS_G, precompute=True)


def _run_stress_job(
    job: tuple[str, int, int, tuple[str, ...], int],
) -> list[dict[str, object]]:
    if _STRESS_G is None or _STRESS_ROUTER is None:
        _init_stress_worker(str(DEFAULT_MAP_FILE))
    assert _STRESS_G is not None
    assert _STRESS_ROUTER is not None
    return _run_stress_job_on_graph(_STRESS_G, _STRESS_ROUTER, job)


def _run_stress_job_on_graph(
    G: nx.DiGraph,
    router: AStarRouter,
    job: tuple[str, int, int, tuple[str, ...], int],
) -> list[dict[str, object]]:
    distribution, num_agents, seed, baselines, max_time = job
    starts, goals = sample_warehouse_stress_instance(
        G,
        num_agents,
        seed,
        distribution,
    )
    rows: list[dict[str, object]] = []
    for baseline in baselines:
        run = run_mapf_baseline(
            G,
            starts,
            goals,
            baseline=baseline,
            seed=seed,
            max_time=max_time,
            use_external=False,
            router=router,
        )
        row = asdict(
            benchmark_row(
                baseline=baseline,
                seed=seed,
                num_agents=num_agents,
                starts=starts,
                goals=run.goals,
                run=run,
                router=router,
            )
        )
        row["distribution"] = distribution
        rows.append(row)
    return rows


def write_cbs_reference_csv(rows: Sequence[CBSReferenceRow], path: Path | str) -> Path:
    output = Path(path)
    output.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = list(CBSReferenceRow.__dataclass_fields__)
    with output.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(asdict(row))
    return output


def write_dict_csv(rows: Sequence[Mapping[str, object]], path: Path | str) -> Path:
    output = Path(path)
    output.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        output.write_text("", encoding="utf-8")
        return output
    fieldnames = list(rows[0])
    with output.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(row)
    return output


def write_summary(
    cbs_rows: Sequence[CBSReferenceRow],
    stress_rows: Sequence[Mapping[str, object]],
    path: Path | str,
) -> Path:
    output = Path(path)
    output.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        "# Sprint 3.5 Gate Evidence Summary",
        "",
        "Generated by `python -m src.baselines.sprint35_gate`.",
        "",
        "## M1.1 cbs-mapf Reference",
        "",
    ]
    lines.extend(_render_cbs_summary(cbs_rows))
    lines.extend(["", "## M1.2 Hard Distribution Stress", ""])
    lines.extend(_render_stress_summary(stress_rows))
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return output


def _render_cbs_summary(rows: Sequence[CBSReferenceRow]) -> list[str]:
    out = [
        "| Scenario | N | attempted | cbs solved | PP-32/CBS mean | PP-32/CBS std | PP-32/LB mean | note |",
        "|---|---:|---:|---:|---:|---:|---:|---|",
    ]
    grouped: dict[tuple[str, int], list[CBSReferenceRow]] = {}
    for row in rows:
        grouped.setdefault((row.scenario, row.num_agents), []).append(row)
    for (scenario, num_agents), items in sorted(grouped.items()):
        solved = [row for row in items if row.cbs_success]
        ratios = [row.pp32_over_cbs for row in solved if math.isfinite(row.pp32_over_cbs)]
        lb_ratios = [
            row.pp32_over_lower_bound
            for row in items
            if row.pp32_success and math.isfinite(row.pp32_over_lower_bound)
        ]
        note = ""
        if not solved:
            reasons = _top_reasons(row.cbs_failure_reason for row in items)
            note = f"no cbs-mapf reference; failures: {reasons}"
        out.append(
            "| {scenario} | {num_agents} | {attempted} | {solved_count} | {mean_ratio} | {std_ratio} | {mean_lb} | {note} |".format(
                scenario=scenario,
                num_agents=num_agents,
                attempted=len(items),
                solved_count=len(solved),
                mean_ratio=_fmt_mean(ratios),
                std_ratio=_fmt_std(ratios),
                mean_lb=_fmt_mean(lb_ratios),
                note=note,
            )
        )
    return out


def _render_stress_summary(rows: Sequence[Mapping[str, object]]) -> list[str]:
    out = [
        "| Distribution | Baseline | N | runs | success | mean makespan success | mean ratio success | dominant failure |",
        "|---|---|---:|---:|---:|---:|---:|---|",
    ]
    grouped: dict[tuple[str, str, int], list[Mapping[str, object]]] = {}
    for row in rows:
        key = (
            str(row["distribution"]),
            str(row["baseline"]),
            int(row["num_agents"]),
        )
        grouped.setdefault(key, []).append(row)
    for (distribution, baseline, num_agents), items in sorted(grouped.items()):
        successful = [row for row in items if _as_bool(row["success"])]
        makespans = [float(row["instance_makespan"]) for row in successful]
        ratios = [
            float(row["makespan_over_lower_bound"])
            for row in successful
            if math.isfinite(float(row["makespan_over_lower_bound"]))
        ]
        failures = [
            str(row["failure_reason"])
            for row in items
            if not _as_bool(row["success"]) and str(row["failure_reason"])
        ]
        out.append(
            "| {distribution} | {baseline} | {num_agents} | {runs} | {successes} | {makespan} | {ratio} | {failure} |".format(
                distribution=distribution,
                baseline=baseline,
                num_agents=num_agents,
                runs=len(items),
                successes=len(successful),
                makespan=_fmt_mean(makespans),
                ratio=_fmt_mean(ratios),
                failure=_top_reasons(failures),
            )
        )
    return out


def _sample_distinct_nodes(
    G: nx.DiGraph,
    num_agents: int,
    seed: int,
) -> tuple[dict[str, str], dict[str, str]]:
    rng = np.random.default_rng(seed)
    nodes = np.array(list(G.nodes()), dtype=object)
    chosen = rng.choice(nodes, size=2 * num_agents, replace=False)
    agents = [f"agv_{idx}" for idx in range(num_agents)]
    return (
        {agent: str(node) for agent, node in zip(agents, chosen[:num_agents])},
        {agent: str(node) for agent, node in zip(agents, chosen[num_agents:])},
    )


def _central_hotspot_pool(G: nx.DiGraph, min_size: int) -> list[str]:
    coords = _coords(G)
    cx = statistics.mean(x for x, _ in coords.values())
    cy = statistics.mean(y for _, y in coords.values())
    ranked = sorted(
        coords,
        key=lambda node: (
            (coords[node][0] - cx) ** 2 + (coords[node][1] - cy) ** 2,
            node,
        ),
    )
    return ranked[:min(min_size, len(ranked))]


def _opposite_side_pools(G: nx.DiGraph, min_size: int) -> tuple[list[str], list[str]]:
    coords = _coords(G)
    ranked_by_x = sorted(coords, key=lambda node: (coords[node][0], coords[node][1], node))
    size = min(min_size, len(ranked_by_x) // 2)
    return ranked_by_x[:size], ranked_by_x[-size:]


def _coords(G: nx.DiGraph) -> dict[str, tuple[float, float]]:
    return {
        str(node): (float(data["x"]), float(data["y"]))
        for node, data in G.nodes(data=True)
    }


def _hungarian_pp(
    G: nx.DiGraph,
    starts: Mapping[str, str],
    goals: Mapping[str, str],
    router: AStarRouter,
    max_time: int,
) -> BaselineRun:
    started = time.perf_counter()
    assignments = hungarian_goal_assignment(starts, list(goals.values()), router)
    elapsed_assignment_s = time.perf_counter() - started
    assigned_goals = {agent: item.goal for agent, item in assignments.items()}
    planner_start = time.perf_counter()
    result = CBSMapfPlanner(G, max_time=max_time).plan(
        starts,
        assigned_goals,
        use_external=False,
    )
    return BaselineRun(
        result=result,
        goals=assigned_goals,
        elapsed_assignment_s=elapsed_assignment_s,
        elapsed_planner_s=time.perf_counter() - planner_start,
    )


def _cbs_reference_row(
    scenario: str,
    distribution: str,
    seed: int,
    num_agents: int,
    version: str,
    starts: Mapping[str, str],
    goals: Mapping[str, str],
    cbs: MAPFPlanResult,
    pp32: MAPFPlanResult,
    default_pp: MAPFPlanResult,
    hungarian_pp: MAPFPlanResult,
    hungarian_cbs: MAPFPlanResult,
    lower_bound_steps: int,
    pp32_elapsed_s: float,
    hungarian_goals: Mapping[str, str],
) -> CBSReferenceRow:
    return CBSReferenceRow(
        scenario=scenario,
        distribution=distribution,
        seed=seed,
        num_agents=num_agents,
        cbs_mapf_version=version,
        cbs_success=cbs.success,
        cbs_solver=cbs.solver,
        cbs_failure_reason=_result_failure_reason(cbs),
        cbs_makespan=cbs.makespan,
        cbs_sum_of_costs=cbs.sum_of_costs,
        cbs_elapsed_s=cbs.elapsed_s,
        lower_bound_steps=lower_bound_steps,
        cbs_over_lower_bound=_ratio(cbs.makespan, lower_bound_steps, cbs.success),
        pp32_success=pp32.success,
        pp32_makespan=pp32.makespan,
        pp32_over_cbs=_ratio(pp32.makespan, cbs.makespan, pp32.success and cbs.success),
        pp32_over_lower_bound=_ratio(pp32.makespan, lower_bound_steps, pp32.success),
        pp32_elapsed_s=pp32_elapsed_s,
        pp32_orders_tried=int(pp32.diagnostics.get("orders_tried", 0)),
        default_pp_success=default_pp.success,
        default_pp_makespan=default_pp.makespan,
        default_pp_over_cbs=_ratio(
            default_pp.makespan,
            cbs.makespan,
            default_pp.success and cbs.success,
        ),
        hungarian_cbs_success=hungarian_cbs.success,
        hungarian_cbs_makespan=hungarian_cbs.makespan,
        hungarian_cbs_failure_reason=_result_failure_reason(hungarian_cbs),
        hungarian_pp_success=hungarian_pp.success,
        hungarian_pp_makespan=hungarian_pp.makespan,
        hungarian_pp_over_hungarian_cbs=_ratio(
            hungarian_pp.makespan,
            hungarian_cbs.makespan,
            hungarian_pp.success and hungarian_cbs.success,
        ),
        starts_json=json.dumps(dict(starts), sort_keys=True),
        goals_json=json.dumps(
            {"direct": dict(goals), "hungarian": dict(hungarian_goals)},
            sort_keys=True,
        ),
        cbs_diagnostics_json=json.dumps(cbs.diagnostics, sort_keys=True),
    )


def _lower_bound_steps(
    starts: Mapping[str, str],
    goals: Mapping[str, str],
    router: AStarRouter,
) -> int:
    out = 0
    for agent, start in starts.items():
        path = router.path(start, goals[agent])
        if path is None:
            return 0
        out = max(out, len(path) - 1)
    return out


def _ratio(value: int, reference: int, enabled: bool) -> float:
    if not enabled or reference <= 0:
        return math.inf
    return value / reference


def _result_failure_reason(result: MAPFPlanResult) -> str:
    if result.success:
        return ""
    if result.conflicts:
        return result.conflicts[0].type
    return str(result.diagnostics.get("error", "planner_failure"))


def _as_bool(value: object) -> bool:
    if isinstance(value, bool):
        return value
    return str(value).lower() in {"1", "true", "yes"}


def _fmt_mean(values: Sequence[float]) -> str:
    finite = [value for value in values if math.isfinite(value)]
    if not finite:
        return "NA"
    return f"{statistics.mean(finite):.3f}"


def _fmt_std(values: Sequence[float]) -> str:
    finite = [value for value in values if math.isfinite(value)]
    if len(finite) < 2:
        return "0.000" if finite else "NA"
    return f"{statistics.stdev(finite):.3f}"


def _top_reasons(reasons: Iterable[str]) -> str:
    counts: dict[str, int] = {}
    for reason in reasons:
        if not reason:
            continue
        counts[reason] = counts.get(reason, 0) + 1
    if not counts:
        return ""
    reason, count = max(counts.items(), key=lambda item: (item[1], item[0]))
    return f"{reason} ({count})"


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--seeds", type=int, nargs="+", default=list(DEFAULT_SEEDS))
    parser.add_argument("--cbs-agents", type=int, nargs="+", default=list(DEFAULT_CBS_COUNTS))
    parser.add_argument("--stress-agents", type=int, nargs="+", default=list(DEFAULT_STRESS_COUNTS))
    parser.add_argument("--stress-seeds", type=int, nargs="+", default=None)
    parser.add_argument("--stress-baselines", nargs="+", default=list(DEFAULT_BASELINES))
    parser.add_argument("--cbs-max-process", type=int, default=1)
    parser.add_argument("--cbs-jobs", type=int, default=1)
    parser.add_argument("--cbs-max-iter", type=int, default=200)
    parser.add_argument("--cbs-low-level-max-iter", type=int, default=300)
    parser.add_argument("--jobs", type=int, default=1)
    parser.add_argument("--skip-cbs", action="store_true")
    parser.add_argument("--skip-warehouse-cbs-probe", action="store_true")
    parser.add_argument("--skip-stress", action="store_true")
    args = parser.parse_args(argv)

    cbs_rows = []
    if not args.skip_cbs:
        cbs_rows = run_cbs_reference(
            seeds=args.seeds,
            agent_counts=args.cbs_agents,
            warehouse_probe=not args.skip_warehouse_cbs_probe,
            cbs_max_iter=args.cbs_max_iter,
            cbs_low_level_max_iter=args.cbs_low_level_max_iter,
            cbs_max_process=args.cbs_max_process,
            cbs_jobs=args.cbs_jobs,
        )
    cbs_csv = write_cbs_reference_csv(
        cbs_rows,
        args.output_dir / "sprint35_cbs_reference.csv",
    )
    print(f"Wrote {len(cbs_rows)} CBS reference rows to {cbs_csv}")

    stress_rows = []
    if not args.skip_stress:
        stress_rows = run_stress(
            seeds=args.stress_seeds if args.stress_seeds is not None else args.seeds,
            agent_counts=args.stress_agents,
            baselines=args.stress_baselines,
            jobs=args.jobs,
        )

    stress_csv = write_dict_csv(
        stress_rows,
        args.output_dir / "sprint35_stress.csv",
    )
    summary = write_summary(
        cbs_rows,
        stress_rows,
        args.output_dir / "sprint35_gate_summary.md",
    )
    print(f"Wrote {len(stress_rows)} stress rows to {stress_csv}")
    print(f"Wrote summary to {summary}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
