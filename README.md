# IPPO MAPF / MAPD Warehouse Simulator

Python tooling for parsing an OpenTCS warehouse map, running graph routing/MAPF
baselines, visualizing topology, and logging MAPD rollout metrics.

## Setup

Install the core dependencies:

```powershell
pip install -r requirements.txt
```

The external `cbs-mapf` backend is optional because the adapter validates every
returned path against the directed OpenTCS graph and falls back to graph-native
prioritized space-time A* when the grid backend is unavailable or invalid.
Install it when working on Sprint 2 CBS baseline smoke tests:

```powershell
pip install cbs-mapf
```

## Verification

Run the test suite from the repo root:

```powershell
python -m unittest discover -s src
```

With `cbs-mapf` installed, the suite should include the external backend smoke
test. Without it, that single smoke test is skipped.

## MAPF Benchmark Logging

Run the Sprint 2 classical baselines and write a CSV:

```powershell
python -m src.baselines.benchmark --agents 10 15 20 --seeds 0 1 2 3 4
```

Use `--no-external` when you want deterministic graph-native fallback only,
without attempting the optional `cbs-mapf` grid backend.
The default CSV uses paper-safe labels for the graph-native baselines:
`prioritized_planning_default_order` and
`hungarian_prioritized_planning` plus
`fifo_nearest_prioritized_planning`. The optional `cbs` / `hungarian_cbs` /
`fifo_nearest` aliases still exist for adapter smoke experiments, but the
warehouse benchmark should not call them "CBS" unless rows actually report
`solver=cbs_mapf`.
The `priority_search` baseline is multi-order prioritized planning, not full
PBS constraint-tree search.
The `opentcs_default_emulator` baseline approximates the default OpenTCS stack
shape for thesis comparison: dispatcher-style cost-greedy assignment, shortest
path routing, and queued resource-allocation waits. It deliberately does not
emulate production deadlock recovery/rerouting, so cyclic waits can time out.

Summarize a raw benchmark CSV into a compact markdown table:

```powershell
python -m src.baselines.aggregate results/baselines/sprint2_mapf_baselines.csv
```

For the thesis table, `lower_bound_steps` is the MAPF-IS lower bound:
`max_i single_agent_shortest_path_steps(start_i, goal_i)`. It is admissible,
but it is not a CBS-optimal makespan. The retained `cbs-mapf` adapter is useful
for smoke checks, while the warehouse benchmark defaults to graph-native
prioritized planners for reproducibility on the directed topology.

`priority_search` exits early on the first successful priority order, so wall
time can vary sharply with agent count and seed. Treat its latency as a
worst-case-budgeted method rather than assuming monotonic scaling.

For one-shot MAPF/CBS baselines, pass the planner makespan into the rollout
logger so the CSV/JSON metrics do not confuse lifelong MAPD completion time
with one-shot MAPF makespan:

```python
metrics = logger.finalize(instance_makespan=plan_result.makespan)
```

`last_completion_step` is for streamed MAPD episodes. `instance_makespan` is for
one-shot MAPF instances.

## Repository Layout

Commit the core project files:

- `src/` - parser, routing, PettingZoo env, MAPF adapter, logging, visualization
- `orca_share_media1778260607027_7458565577098821053.xml` - OpenTCS map sample
- `results/map/` - curated topology visualization artifacts
- `requirements.txt`, `README.md`

The local `cleanrl/` and `opentcs-integration-example/` folders are ignored as
third-party checkouts. Keep them as separate upstream repos, forks, or submodules
if they become part of the final thesis artifact.
`PLAN.md` and `CLAUDE.md` are ignored local planning notes.
