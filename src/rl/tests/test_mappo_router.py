"""Smoke tests for the Sprint 3 vanilla MAPPO router prototype."""

from __future__ import annotations

import math
import unittest

import networkx as nx
import numpy as np
import torch

from src.env.compass_mapper import NUM_ACTIONS
from src.env.warehouse_env import WarehouseEnv
from src.rl.mappo_router import (
    MAPPOConfig,
    MAPPOTrainer,
    RolloutBuffer,
    SharedActor,
    build_global_state,
    stack_agent_observations,
)
from src.routing.astar import AStarRouter


def _grid_graph(width: int = 3, height: int = 3) -> nx.DiGraph:
    graph = nx.DiGraph()
    for y in range(height):
        for x in range(width):
            node = f"{x},{y}"
            graph.add_node(node, x=float(x), y=float(y))
    for y in range(height):
        for x in range(width):
            node = f"{x},{y}"
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx_, ny_ = x + dx, y + dy
                if 0 <= nx_ < width and 0 <= ny_ < height:
                    graph.add_edge(node, f"{nx_},{ny_}", length=1.0)
    return graph


def _tiny_env(seed: int = 0) -> WarehouseEnv:
    graph = _grid_graph()
    router = AStarRouter(graph, precompute=True)
    return WarehouseEnv(
        graph=graph,
        router=router,
        num_agents=2,
        episode_horizon=8,
        task_rate=0.0,
        knn_agents=1,
        seed=seed,
    )


class TestMAPPOModels(unittest.TestCase):
    def test_shared_actor_never_samples_masked_actions(self):
        actor = SharedActor(obs_dim=4, num_actions=NUM_ACTIONS, hidden_dim=16)
        obs = torch.zeros((32, 4), dtype=torch.float32)
        mask = torch.zeros((32, NUM_ACTIONS), dtype=torch.bool)
        mask[:, 3] = True

        actions, log_probs, entropy = actor.act(obs, mask)

        self.assertTrue(torch.equal(actions, torch.full((32,), 3)))
        self.assertTrue(torch.isfinite(log_probs).all())
        self.assertTrue(torch.isfinite(entropy).all())

    def test_observation_packing_uses_explicit_agent_order(self):
        obs = {
            "agent_1": {
                "observation": np.array([1.0, 2.0], dtype=np.float32),
                "action_mask": np.array([1, 0, 1], dtype=np.int8),
            },
            "agent_0": {
                "observation": np.array([3.0, 4.0], dtype=np.float32),
                "action_mask": np.array([0, 1, 1], dtype=np.int8),
            },
        }

        agents, obs_tensor, mask_tensor = stack_agent_observations(
            obs,
            agent_order=["agent_0", "agent_1"],
        )
        global_state = build_global_state(obs, agents)

        self.assertEqual(agents, ["agent_0", "agent_1"])
        self.assertEqual(obs_tensor.tolist(), [[3.0, 4.0], [1.0, 2.0]])
        self.assertEqual(mask_tensor.tolist(), [[False, True, True], [True, False, True]])
        self.assertEqual(global_state.tolist(), [3.0, 4.0, 1.0, 2.0])


class TestRolloutBuffer(unittest.TestCase):
    def test_gae_matches_two_step_team_return(self):
        buffer = RolloutBuffer()
        for reward in (1.0, 1.0):
            buffer.add(
                obs=torch.zeros((2, 3)),
                action_mask=torch.ones((2, NUM_ACTIONS), dtype=torch.bool),
                global_state=torch.zeros(6),
                actions=torch.zeros(2, dtype=torch.long),
                log_probs=torch.zeros(2),
                reward=reward,
                done=False,
                value=torch.tensor(0.0),
            )

        returns, advantages = buffer.compute_returns_and_advantages(
            next_value=torch.tensor(0.0),
            gamma=1.0,
            gae_lambda=1.0,
        )

        self.assertEqual(returns.tolist(), [2.0, 1.0])
        self.assertEqual(advantages.tolist(), [2.0, 1.0])


class TestMAPPOTrainer(unittest.TestCase):
    def test_one_update_runs_and_changes_parameters(self):
        env = _tiny_env(seed=0)
        config = MAPPOConfig(
            num_steps=4,
            update_epochs=1,
            hidden_dim=16,
            learning_rate=1e-3,
            seed=0,
        )
        trainer = MAPPOTrainer(env, config=config)
        before = [param.detach().clone() for param in trainer.actor.parameters()]

        metrics = trainer.train_updates(num_updates=1)[0]

        self.assertTrue(math.isfinite(metrics["loss_total"]))
        self.assertTrue(math.isfinite(metrics["entropy"]))
        changed = any(
            not torch.allclose(old, new.detach())
            for old, new in zip(before, trainer.actor.parameters())
        )
        self.assertTrue(changed, "expected MAPPO update to modify actor parameters")


if __name__ == "__main__":
    unittest.main()
