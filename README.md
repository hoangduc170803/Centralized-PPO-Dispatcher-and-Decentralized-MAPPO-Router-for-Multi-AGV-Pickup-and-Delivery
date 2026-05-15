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

For journal-grade evaluation, increase to at least 20-30 seeds:

```powershell
python -m src.baselines.benchmark --agents 10 15 20 --seeds 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29
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

For paper tables, include confidence intervals on success rate and all-run
penalized metrics so failed runs are not hidden:

```powershell
python -m src.baselines.aggregate results/baselines/sprint2_mapf_baselines.csv --ci bootstrap --bootstrap-samples 10000 --failure-makespan-penalty 512
```

For the thesis table, `lower_bound_steps` is the MAPF-IS lower bound:
`max_i single_agent_shortest_path_steps(start_i, goal_i)`. It is admissible,
but it is not a CBS-optimal makespan. The retained `cbs-mapf` adapter is useful
for smoke checks, while the warehouse benchmark defaults to graph-native
prioritized planners for reproducibility on the directed topology.

`priority_search` exits early on the first successful priority order, so wall
time can vary sharply with agent count and seed. Treat its latency as a
worst-case-budgeted method rather than assuming monotonic scaling.

Makespan, waiting-time, throughput, and ratio summaries are computed on
successful runs only. Always present these charts together with success rate;
otherwise low-success methods can look deceptively fast.

For one-shot MAPF/CBS baselines, pass the planner makespan into the rollout
logger so the CSV/JSON metrics do not confuse lifelong MAPD completion time
with one-shot MAPF makespan:

```python
metrics = logger.finalize(instance_makespan=plan_result.makespan)
```

`last_completion_step` is for streamed MAPD episodes. `instance_makespan` is for
one-shot MAPF instances.

## Sprint 3 MAPPO Training (marlbenchmark/on-policy backend)

The Sprint 3 MAPPO Router stack is built on the official MAPPO implementation
([marlbenchmark/on-policy](https://github.com/marlbenchmark/on-policy)), which
ships PopArt / ValueNorm value normalization, per-agent advantage with a
centralized critic, mini-batch SGD, KL-aware PPO updates, and built-in action
masks (`available_actions`). The full upstream repo is vendored under
`on-policy/` at commit `de66d7a4b23fac2513f56f96f73b3f5cb96695ac`; see
[on-policy/PATCHES.md](on-policy/PATCHES.md) for the small compatibility patches.

Tensorboard logs and pinned dependencies install with the base requirements
(`tensorboardX>=2.6`). No additional pip install is required for the vendored
package; `src.rl.mappo_onpolicy` puts `on-policy/` on `sys.path` automatically.

Run a 5-AGV, 200k env-step smoke training on the largest-SCC warehouse map:

```powershell
python -m src.rl.mappo_onpolicy.train `
    --num_agents_target 5 `
    --episode_length 128 `
    --num_env_steps 200000 `
    --hidden_size 64 --layer_N 2 `
    --experiment_name sprint3_smoke_5agv --seed 0
```

The Sprint 3 defaults use a conservative PPO update budget
(`ppo_epoch=4`, `num_mini_batch=4`) and higher exploration pressure
(`entropy_coef=0.03`) after the first 200k-step run showed early entropy
collapse.
Action masks also include a conservative one-step look-ahead by default:
graph-invalid moves are masked, and so are moves into currently occupied nodes,
nodes another AGV is predicted to occupy next, or reverse traversal of a
predicted edge. Use `--disable_lookahead_action_mask` only for ablations.
At higher density, watch `warehouse/lookahead_forced_wait_rate`; a rising value
means the mask is over-constraining agents into WAIT and may need a yield policy.

Treat 200k env steps as a smoke run. For thesis-grade 5-AGV learning curves,
budget at least 1-5M env steps and compare multiple seeds:

```powershell
python -m src.rl.mappo_onpolicy.train `
    --num_agents_target 5 `
    --episode_length 128 `
    --num_env_steps 1000000 `
    --hidden_size 64 --layer_N 2 `
    --experiment_name sprint3_5agv_1m --seed 0
```

For W8 go/no-go or thesis tables, use at least 20 seeds so the error bars are
meaningful. The helper script runs repeated seeds with identical settings:

```powershell
.\scripts\run_sprint3_1m.ps1 -SeedCsv 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19
```

Logs land in `results/sprint3/onpolicy_smoke/<experiment>_seed<seed>/`:

- `logs/` - tensorboardX scalars per tag (open with `tensorboard --logdir`).
- `models/actor.pt`, `models/critic.pt` - latest checkpoint pair.

Extract scalars from the tfevents files into a per-tag JSON summary plus a
long-form CSV (no tensorboard server needed):

```powershell
python -m src.rl.mappo_onpolicy.read_metrics `
    --log-dir results/sprint3/onpolicy_smoke/sprint3_smoke_5agv_seed0/logs
```

Key tags include `average_step_rewards`, `dist_entropy`, `value_loss`,
`policy_loss`, `ratio`, plus warehouse-specific aggregates under
`warehouse/tasks_completed_total`, `warehouse/validator_intervention_rate`,
`warehouse/lookahead_forced_wait_rate`, and per-step conflict counters.

## Repository Layout

Commit the core project files:

- `src/` - parser, routing, PettingZoo env, MAPF adapter, logging, visualization
- `orca_share_media1778260607027_7458565577098821053.xml` - OpenTCS map sample
- `results/map/` - curated topology visualization artifacts
- `requirements.txt`, `README.md`

The local `cleanrl/` and `opentcs-integration-example/` folders are ignored as
third-party checkouts. Keep them as separate upstream repos, forks, or submodules
if they become part of the final thesis artifact. The `on-policy/` checkout is
tracked (with patches documented in `on-policy/PATCHES.md`), but its runtime
outputs (`on-policy/runs/`, `on-policy/wandb/`, pycache) are ignored.
