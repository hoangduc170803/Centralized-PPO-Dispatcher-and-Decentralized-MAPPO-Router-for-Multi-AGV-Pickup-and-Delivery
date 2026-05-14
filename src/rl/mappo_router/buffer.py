"""Rollout storage and GAE for the Sprint 3 MAPPO prototype."""

from __future__ import annotations

from dataclasses import dataclass

import torch


@dataclass
class RolloutBatch:
    obs: torch.Tensor
    action_masks: torch.Tensor
    global_states: torch.Tensor
    actions: torch.Tensor
    old_log_probs: torch.Tensor
    advantages: torch.Tensor
    returns: torch.Tensor
    values: torch.Tensor


class RolloutBuffer:
    """Fixed-length team-reward rollout buffer.

    The critic is V(global_state), so rewards/values/returns are per timestep.
    Actor log-probs/actions are per agent and use the same timestep advantage.
    """

    def __init__(self):
        self.obs: list[torch.Tensor] = []
        self.action_masks: list[torch.Tensor] = []
        self.global_states: list[torch.Tensor] = []
        self.actions: list[torch.Tensor] = []
        self.log_probs: list[torch.Tensor] = []
        self.rewards: list[float] = []
        self.dones: list[float] = []
        self.values: list[torch.Tensor] = []

    def add(
        self,
        obs: torch.Tensor,
        action_mask: torch.Tensor,
        global_state: torch.Tensor,
        actions: torch.Tensor,
        log_probs: torch.Tensor,
        reward: float,
        done: bool,
        value: torch.Tensor,
    ) -> None:
        self.obs.append(obs.detach().cpu())
        self.action_masks.append(action_mask.detach().cpu())
        self.global_states.append(global_state.detach().cpu())
        self.actions.append(actions.detach().cpu())
        self.log_probs.append(log_probs.detach().cpu())
        self.rewards.append(float(reward))
        self.dones.append(1.0 if done else 0.0)
        self.values.append(value.detach().reshape(()).cpu())

    def __len__(self) -> int:
        return len(self.rewards)

    def compute_returns_and_advantages(
        self,
        next_value: torch.Tensor,
        gamma: float,
        gae_lambda: float,
    ) -> tuple[torch.Tensor, torch.Tensor]:
        if not self.rewards:
            raise ValueError("cannot compute GAE on an empty buffer")

        values = torch.stack(self.values)
        rewards = torch.tensor(self.rewards, dtype=torch.float32)
        dones = torch.tensor(self.dones, dtype=torch.float32)
        advantages = torch.zeros_like(rewards)
        last_gae = torch.tensor(0.0)
        next_value_cpu = next_value.detach().reshape(()).cpu()

        for step in reversed(range(len(rewards))):
            if step == len(rewards) - 1:
                next_non_terminal = 1.0 - dones[step]
                next_val = next_value_cpu
            else:
                next_non_terminal = 1.0 - dones[step]
                next_val = values[step + 1]
            delta = rewards[step] + gamma * next_val * next_non_terminal - values[step]
            last_gae = delta + gamma * gae_lambda * next_non_terminal * last_gae
            advantages[step] = last_gae

        returns = advantages + values
        return returns, advantages

    def as_batch(
        self,
        next_value: torch.Tensor,
        gamma: float,
        gae_lambda: float,
        normalize_advantages: bool = True,
        device: torch.device | str | None = None,
    ) -> RolloutBatch:
        returns, advantages = self.compute_returns_and_advantages(
            next_value=next_value,
            gamma=gamma,
            gae_lambda=gae_lambda,
        )
        if normalize_advantages and len(advantages) > 1:
            std = advantages.std(unbiased=False)
            if std > 1e-8:
                advantages = (advantages - advantages.mean()) / (std + 1e-8)

        return RolloutBatch(
            obs=torch.stack(self.obs).to(device),
            action_masks=torch.stack(self.action_masks).to(device),
            global_states=torch.stack(self.global_states).to(device),
            actions=torch.stack(self.actions).to(device),
            old_log_probs=torch.stack(self.log_probs).to(device),
            advantages=advantages.to(device),
            returns=returns.to(device),
            values=torch.stack(self.values).to(device),
        )
