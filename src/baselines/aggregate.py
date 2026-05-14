"""Aggregate Sprint 2 benchmark CSV rows into a compact summary table."""

from __future__ import annotations

import argparse
import csv
import math
from collections import Counter, defaultdict
from pathlib import Path
from statistics import mean, stdev
from typing import Iterable, Sequence


def aggregate_rows(rows: Iterable[dict[str, str]]) -> list[dict[str, object]]:
    """Group raw benchmark rows by baseline and agent count."""
    grouped: dict[tuple[str, int], list[dict[str, str]]] = defaultdict(list)
    for row in rows:
        grouped[(row["baseline"], int(row["num_agents"]))].append(row)

    summary: list[dict[str, object]] = []
    for (baseline, num_agents), items in sorted(grouped.items()):
        successes = [_as_bool(row["success"]) for row in items]
        successful_rows = [row for row, ok in zip(items, successes) if ok]
        failure_reasons = Counter(
            row.get("failure_reason", "") or "unspecified"
            for row, ok in zip(items, successes)
            if not ok
        )
        summary.append(
            {
                "baseline": baseline,
                "num_agents": num_agents,
                "runs": len(items),
                "success_rate": mean(1.0 if ok else 0.0 for ok in successes),
                "mean_makespan_success": _mean_field(successful_rows, "instance_makespan"),
                "std_makespan_success": _std_field(successful_rows, "instance_makespan"),
                "mean_ratio_success": _mean_field(successful_rows, "makespan_over_lower_bound"),
                "std_ratio_success": _std_field(successful_rows, "makespan_over_lower_bound"),
                "mean_wait_success": _mean_field(successful_rows, "waiting_time_agent_steps"),
                "std_wait_success": _std_field(successful_rows, "waiting_time_agent_steps"),
                "mean_elapsed_s": _mean_field(items, "elapsed_s"),
                "failure_reasons": ", ".join(
                    f"{reason}:{count}" for reason, count in sorted(failure_reasons.items())
                ),
            }
        )
    return summary


def render_markdown(summary: Sequence[dict[str, object]]) -> str:
    """Render aggregate rows as a markdown table."""
    headers = [
        "baseline",
        "agents",
        "runs",
        "success",
        "mean_makespan_success",
        "std_makespan_success",
        "mean_ratio_success",
        "std_ratio_success",
        "mean_wait_success",
        "std_wait_success",
        "mean_elapsed_s",
        "failure_reasons",
    ]
    lines = [
        "| " + " | ".join(headers) + " |",
        "| " + " | ".join(["---"] * len(headers)) + " |",
    ]
    for row in summary:
        lines.append(
            "| "
            + " | ".join(
                [
                    str(row["baseline"]),
                    str(row["num_agents"]),
                    str(row["runs"]),
                    _fmt_float(row["success_rate"]),
                    _fmt_float(row["mean_makespan_success"]),
                    _fmt_float(row["std_makespan_success"]),
                    _fmt_float(row["mean_ratio_success"]),
                    _fmt_float(row["std_ratio_success"]),
                    _fmt_float(row["mean_wait_success"]),
                    _fmt_float(row["std_wait_success"]),
                    _fmt_float(row["mean_elapsed_s"], digits=4),
                    str(row["failure_reasons"]),
                ]
            )
            + " |"
        )
    return "\n".join(lines)


def read_csv(path: Path | str) -> list[dict[str, str]]:
    with Path(path).open("r", newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def _mean_field(rows: Sequence[dict[str, str]], field: str) -> float:
    values = _finite_field_values(rows, field)
    return mean(values) if values else math.nan


def _std_field(rows: Sequence[dict[str, str]], field: str) -> float:
    values = _finite_field_values(rows, field)
    return stdev(values) if len(values) >= 2 else math.nan


def _finite_field_values(rows: Sequence[dict[str, str]], field: str) -> list[float]:
    values: list[float] = []
    for row in rows:
        raw = row.get(field, "")
        try:
            value = float(raw)
        except (TypeError, ValueError):
            continue
        if math.isfinite(value):
            values.append(value)
    return values


def _as_bool(raw: str) -> bool:
    return raw.strip().lower() in {"true", "1", "yes"}


def _fmt_float(value: object, digits: int = 3) -> str:
    try:
        as_float = float(value)
    except (TypeError, ValueError):
        return ""
    if not math.isfinite(as_float):
        return ""
    return f"{as_float:.{digits}f}"


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv_path", type=Path)
    args = parser.parse_args(argv)
    summary = aggregate_rows(read_csv(args.csv_path))
    print(render_markdown(summary))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
