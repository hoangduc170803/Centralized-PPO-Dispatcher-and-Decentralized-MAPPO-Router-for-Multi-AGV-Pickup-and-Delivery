"""Poisson task generator for pickup-and-delivery (MAPD).

Each step, sample `n_new ~ Poisson(rate)` new tasks. A task is a (pickup,
dropoff) pair drawn uniformly from the eligible node pool (typically the
largest SCC). The pool is FIFO with a max capacity to prevent unbounded growth
when agents fall behind.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Optional

import numpy as np


@dataclass
class Task:
    """A pickup-and-delivery task."""

    id: int
    pickup: str
    dropoff: str
    spawn_step: int
    priority: int = 0
    deadline: Optional[int] = None

    # Lifecycle bookkeeping (mutated by env)
    assigned_to: Optional[str] = None   # agent id or None
    picked_up: bool = False
    completed_step: Optional[int] = None

    @property
    def current_goal(self) -> str:
        """Where the assigned agent should head right now."""
        return self.dropoff if self.picked_up else self.pickup


class PoissonTaskGenerator:
    """Spawn tasks at Poisson rate. Also owns the pending-task pool."""

    def __init__(
        self,
        node_pool: list[str],
        rate: float = 0.1,
        max_pool_size: int = 50,
        rng: Optional[np.random.Generator] = None,
    ):
        if rate < 0:
            raise ValueError("rate must be >= 0")
        if len(node_pool) < 2:
            raise ValueError("node_pool needs at least 2 nodes")
        self.node_pool = node_pool
        self.rate = rate
        self.max_pool_size = max_pool_size
        self.rng = rng if rng is not None else np.random.default_rng()

        self._next_id = 0
        self.pending: list[Task] = []
        self.in_flight: dict[int, Task] = {}   # task_id -> Task (assigned/active)
        self.completed: list[Task] = []

    def reset(self, rng: Optional[np.random.Generator] = None):
        if rng is not None:
            self.rng = rng
        self._next_id = 0
        self.pending.clear()
        self.in_flight.clear()
        self.completed.clear()

    def step(self, step_idx: int) -> list[Task]:
        """Spawn N new tasks where N ~ Poisson(rate). Returns the new tasks.

        Pool size is capped at `max_pool_size`; excess tasks are dropped.
        """
        n_new = int(self.rng.poisson(self.rate))
        spawned: list[Task] = []
        for _ in range(n_new):
            if len(self.pending) + len(self.in_flight) >= self.max_pool_size:
                break
            pickup, dropoff = self.rng.choice(self.node_pool, size=2, replace=False)
            t = Task(
                id=self._next_id,
                pickup=str(pickup),
                dropoff=str(dropoff),
                spawn_step=step_idx,
            )
            self._next_id += 1
            self.pending.append(t)
            spawned.append(t)
        return spawned

    def assign(self, task: Task, agent_id: str):
        if task not in self.pending:
            raise ValueError(f"Task {task.id} not in pending pool")
        self.pending.remove(task)
        task.assigned_to = agent_id
        self.in_flight[task.id] = task

    def mark_picked_up(self, task_id: int):
        self.in_flight[task_id].picked_up = True

    def complete(self, task_id: int, step_idx: int):
        task = self.in_flight.pop(task_id)
        task.completed_step = step_idx
        self.completed.append(task)
        return task

    def num_pending(self) -> int:
        return len(self.pending)

    def num_in_flight(self) -> int:
        return len(self.in_flight)

    def num_completed(self) -> int:
        return len(self.completed)
