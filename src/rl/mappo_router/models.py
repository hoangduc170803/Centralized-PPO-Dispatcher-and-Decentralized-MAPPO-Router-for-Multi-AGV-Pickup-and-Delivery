"""Neural modules and tensor packing helpers for vanilla MAPPO."""

from __future__ import annotations

from typing import Mapping, Sequence

import numpy as np
import torch
from torch import nn
from torch.distributions import Categorical


class SharedActor(nn.Module):
    """Parameter-shared decentralized actor with action-mask support."""

    def __init__(
        self,
        obs_dim: int,
        num_actions: int,
        hidden_dim: int = 128,
    ):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(obs_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, num_actions),
        )

    def logits(self, obs: torch.Tensor, action_mask: torch.Tensor) -> torch.Tensor:
        raw_logits = self.net(obs)
        return mask_logits(raw_logits, action_mask)

    def distribution(self, obs: torch.Tensor, action_mask: torch.Tensor) -> Categorical:
        return Categorical(logits=self.logits(obs, action_mask))

    def act(
        self,
        obs: torch.Tensor,
        action_mask: torch.Tensor,
        deterministic: bool = False,
    ) -> tuple[torch.Tensor, torch.Tensor, torch.Tensor]:
        dist = self.distribution(obs, action_mask)
        actions = torch.argmax(dist.probs, dim=-1) if deterministic else dist.sample()
        log_probs = dist.log_prob(actions)
        entropy = dist.entropy()
        return actions, log_probs, entropy

    def evaluate_actions(
        self,
        obs: torch.Tensor,
        action_mask: torch.Tensor,
        actions: torch.Tensor,
    ) -> tuple[torch.Tensor, torch.Tensor]:
        dist = self.distribution(obs, action_mask)
        return dist.log_prob(actions), dist.entropy()


class CentralizedCritic(nn.Module):
    """Centralized scalar value function V(s_global)."""

    def __init__(self, global_state_dim: int, hidden_dim: int = 128):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(global_state_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, 1),
        )

    def forward(self, global_state: torch.Tensor) -> torch.Tensor:
        return self.net(global_state).squeeze(-1)


def mask_logits(logits: torch.Tensor, action_mask: torch.Tensor) -> torch.Tensor:
    """Apply an exact invalid-action mask before Categorical construction."""
    mask = action_mask.to(dtype=torch.bool)
    neg_inf = torch.finfo(logits.dtype).min
    return logits.masked_fill(~mask, neg_inf)


def stack_agent_observations(
    obs: Mapping[str, Mapping[str, np.ndarray]],
    agent_order: Sequence[str] | None = None,
    device: torch.device | str | None = None,
) -> tuple[list[str], torch.Tensor, torch.Tensor]:
    """Stack PettingZoo observation dicts into actor tensors."""
    agents = list(agent_order) if agent_order is not None else sorted(obs)
    obs_tensor = torch.as_tensor(
        np.stack([obs[agent]["observation"] for agent in agents]),
        dtype=torch.float32,
        device=device,
    )
    mask_tensor = torch.as_tensor(
        np.stack([obs[agent]["action_mask"] for agent in agents]),
        dtype=torch.bool,
        device=device,
    )
    return agents, obs_tensor, mask_tensor


def build_global_state(
    obs: Mapping[str, Mapping[str, np.ndarray]],
    agent_order: Sequence[str],
    device: torch.device | str | None = None,
) -> torch.Tensor:
    """Concatenate per-agent local observations into one critic state."""
    flat = np.concatenate([obs[agent]["observation"] for agent in agent_order])
    return torch.as_tensor(flat, dtype=torch.float32, device=device)
