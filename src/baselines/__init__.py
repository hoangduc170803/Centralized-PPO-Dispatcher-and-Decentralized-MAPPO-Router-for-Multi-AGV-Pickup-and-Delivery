"""Classical Sprint 2 baselines for assignment and MAPF benchmarking."""

from .dispatchers import (
    GoalAssignment,
    TaskAssignment,
    fifo_nearest_goal_assignment,
    fifo_nearest_task_assignment,
    hungarian_goal_assignment,
    hungarian_task_assignment,
)

__all__ = [
    "GoalAssignment",
    "TaskAssignment",
    "fifo_nearest_goal_assignment",
    "fifo_nearest_task_assignment",
    "hungarian_goal_assignment",
    "hungarian_task_assignment",
]
