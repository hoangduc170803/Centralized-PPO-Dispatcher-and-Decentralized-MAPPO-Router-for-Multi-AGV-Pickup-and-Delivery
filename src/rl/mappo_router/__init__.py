"""Vanilla MLP MAPPO router prototype for Sprint 3."""

from .buffer import RolloutBatch, RolloutBuffer
from .models import CentralizedCritic, SharedActor, build_global_state, stack_agent_observations

__all__ = [
    "CentralizedCritic",
    "MAPPOConfig",
    "MAPPOTrainer",
    "RolloutBatch",
    "RolloutBuffer",
    "SharedActor",
    "build_global_state",
    "stack_agent_observations",
]


def __getattr__(name: str):
    if name in {"MAPPOConfig", "MAPPOTrainer"}:
        from .trainer import MAPPOConfig, MAPPOTrainer

        return {"MAPPOConfig": MAPPOConfig, "MAPPOTrainer": MAPPOTrainer}[name]
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
