"""Smoke + property tests for WarehouseEnv.

Run from `src/`:
    python -m unittest env.tests.test_warehouse_env
"""

from __future__ import annotations

import math
import sys
import time
import unittest
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from env.compass_mapper import NUM_ACTIONS, WAIT_SLOT
from env.reward_shaper import RewardConfig
from env.warehouse_env import AgentState, WarehouseEnv
from map_parser import parse_opentcs_map
from routing.astar import AStarRouter

# Repo root = parents[2] is `src/`, so go one level up. Keeps the test runnable
# on Linux/macOS/CI without relying on a Windows-specific absolute path.
MAP_FILE = str(
    Path(__file__).resolve().parents[3]
    / "orca_share_media1778260607027_7458565577098821053.xml"
)


def _make_env(num_agents=5, horizon=64, seed=0):
    G = parse_opentcs_map(MAP_FILE, restrict_to_largest_scc=True)
    router = AStarRouter(G, precompute=True)
    return WarehouseEnv(
        graph=G,
        router=router,
        num_agents=num_agents,
        episode_horizon=horizon,
        task_rate=0.1,
        seed=seed,
    )


class TestWarehouseEnv(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.env = _make_env(num_agents=5, horizon=64)

    def test_reset_returns_valid_obs_for_all_agents(self):
        obs, infos = self.env.reset(seed=0)
        self.assertEqual(set(obs.keys()), set(self.env.possible_agents))
        for a, agent_obs in obs.items():
            self.assertIn("observation", agent_obs)
            self.assertIn("action_mask", agent_obs)
            self.assertEqual(agent_obs["observation"].shape, (self.env._obs_dim,))
            self.assertEqual(agent_obs["action_mask"].shape, (NUM_ACTIONS,))
            # WAIT must always be a valid action
            self.assertEqual(int(agent_obs["action_mask"][WAIT_SLOT]), 1)

    def test_step_with_random_actions_runs_full_horizon(self):
        env = _make_env(num_agents=5, horizon=32, seed=1)
        obs, _ = env.reset()
        rng = np.random.default_rng(123)

        for _ in range(32):
            actions = {}
            for a, agent_obs in obs.items():
                valid = np.where(agent_obs["action_mask"] == 1)[0]
                actions[a] = int(rng.choice(valid))
            obs, rewards, terminated, truncated, infos = env.step(actions)
            # Sanity: rewards are finite scalars
            for a, r in rewards.items():
                self.assertTrue(np.isfinite(r), f"non-finite reward for {a}")

        # After horizon, all should be truncated
        self.assertTrue(all(truncated.values()))

    def test_step_following_astar_hint(self):
        """Following the A* hint should complete tasks within a 512-step horizon."""
        env = _make_env(num_agents=5, horizon=512, seed=2)
        obs, _ = env.reset()

        completed = 0
        for _ in range(512):
            actions = {}
            for a, agent_obs in obs.items():
                # The A* one-hot lives at positions 5..5+NUM_ACTIONS in obs.
                hint_oh = agent_obs["observation"][5 : 5 + NUM_ACTIONS]
                hint_slot = int(np.argmax(hint_oh))
                if agent_obs["action_mask"][hint_slot] == 1:
                    actions[a] = hint_slot
                else:
                    actions[a] = WAIT_SLOT
            obs, _, _, truncated, infos = env.step(actions)
            completed = list(infos.values())[0]["tasks_completed_total"]
            if all(truncated.values()):
                break

        self.assertGreater(
            completed, 0,
            f"expected at least 1 task completed via A* hint, got {completed}"
        )

    def test_observation_mask_consistency(self):
        env = _make_env(num_agents=5, horizon=8, seed=3)
        obs, _ = env.reset()
        for a, agent_obs in obs.items():
            # action_mask must have at least WAIT valid
            self.assertGreaterEqual(int(agent_obs["action_mask"].sum()), 1)

    def test_steps_per_second_throughput(self):
        env = _make_env(num_agents=5, horizon=512, seed=4)
        obs, _ = env.reset()
        rng = np.random.default_rng(456)
        t0 = time.perf_counter()
        for _ in range(512):
            actions = {}
            for a, agent_obs in obs.items():
                valid = np.where(agent_obs["action_mask"] == 1)[0]
                actions[a] = int(rng.choice(valid))
            obs, _, _, truncated, _ = env.step(actions)
            if all(truncated.values()):
                break
        elapsed = time.perf_counter() - t0
        sps = 512 / elapsed
        # No hard assertion (CI variance), just print for visibility
        print(f"\n  Throughput: {sps:.0f} env steps/sec ({elapsed*1000:.0f} ms / 512 steps)")
        self.assertGreater(sps, 50, f"env too slow: {sps:.0f} sps")

    # ------------------------------------------------------------------ Sprint 1 fixes

    def test_progress_reward_no_spike_at_transition(self):
        """At a pickup transition, progress shaping must compare distances to
        the SAME goal (snapshot taken before the state flip), not pickup-vs-
        dropoff. Without the snapshot fix, reward would explode (~+/-20) when
        the goal flips mid-step on a map with raw edge weights ~1000.
        """
        env = _make_env(num_agents=2, horizon=8, seed=42)
        env.reset(seed=42)

        # Pick any node with at least one outgoing slot (not WAIT)
        src_node = None
        move_slot = None
        target_node = None
        for n in env._nodes:
            _, s2n = env.compass.get(n)
            for slot, nbr in s2n.items():
                if slot != WAIT_SLOT and nbr != n:
                    src_node, move_slot, target_node = n, slot, nbr
                    break
            if src_node is not None:
                break
        self.assertIsNotNone(src_node, "no movable node found in map")

        # Pick a far dropoff so the bug (pre-fix) would produce a huge spike.
        # We just need ANY node distinct from src and target — pick the one
        # with the largest distance from target_node.
        far_node = None
        far_d = -1.0
        for n in env._nodes:
            if n in (src_node, target_node):
                continue
            d = env.router.distance(target_node, n)
            if math.isfinite(d) and d > far_d:
                far_d = d
                far_node = n
        self.assertIsNotNone(far_node, "no reachable dropoff node found")
        self.assertGreater(far_d, 0.0)

        # Engineer agent_0 at src_node with a pickup-bound task to target_node.
        # Register the task in the generator's in_flight pool so the env's
        # mark_picked_up call doesn't KeyError.
        from env.task_generator import Task
        agent_name = env.agents[0]
        info = env._agent_info[agent_name]
        task = Task(
            id=99999,
            pickup=target_node,
            dropoff=far_node,
            spawn_step=0,
            assigned_to=agent_name,
            picked_up=False,
        )
        env._task_gen.in_flight[task.id] = task
        info.task = task
        info.state = AgentState.TO_PICKUP
        info.pos = src_node
        info.prev_pos = src_node
        info.age = 0.0
        info.age_idle = 0.0

        # Park the other agent somewhere unrelated so it doesn't conflict
        other = env.agents[1]
        for n in env._nodes:
            if n != src_node and n != target_node:
                env._agent_info[other].pos = n
                env._agent_info[other].prev_pos = n
                break
        env._agent_info[other].task = None
        env._agent_info[other].state = AgentState.IDLE

        actions = {agent_name: int(move_slot), other: WAIT_SLOT}
        obs, rewards, _, _, _ = env.step(actions)

        r = rewards[agent_name]
        # Pickup just happened → r_pickup should be in there.
        # With snapshot fix: delta = dist(src, pickup) - 0; normalised → ~O(1/diam).
        # Total reward ≈ r_step + r_pickup + r_progress_coef * normalised_delta.
        # Bounds: must be far below the unfixed magnitude (which would be
        #   r_pickup + r_step + 0.02 * (dist_to_pickup - dist_pickup_to_dropoff)
        # potentially down to -20 or up to +20 depending on layout.)
        self.assertTrue(np.isfinite(r))
        self.assertGreater(
            r, env.cfg.r_pickup * 0.5,
            f"pickup reward should reflect r_pickup gain, got {r}"
        )
        self.assertLess(
            abs(r), 1.5,
            f"pickup-step reward magnitude too large (snapshot bug suspected): {r}"
        )
        # State must have flipped to TO_DROPOFF
        self.assertEqual(info.state, AgentState.TO_DROPOFF)
        self.assertTrue(info.task.picked_up)

    def test_progress_reward_bounded(self):
        """Per-step rewards must stay bounded after distance normalisation.
        Pre-fix, a single step could yield |r| ~20 due to raw edge weights.
        """
        env = _make_env(num_agents=5, horizon=256, seed=11)
        obs, _ = env.reset()
        rng = np.random.default_rng(11)
        max_abs = 0.0
        for _ in range(256):
            actions = {}
            for a, agent_obs in obs.items():
                valid = np.where(agent_obs["action_mask"] == 1)[0]
                actions[a] = int(rng.choice(valid))
            obs, rewards, _, truncated, _ = env.step(actions)
            for r in rewards.values():
                self.assertTrue(np.isfinite(r))
                max_abs = max(max_abs, abs(r))
            if all(truncated.values()):
                break
        # Generous ceiling: r_goal=1.0 + r_pickup=0.2 + r_progress<=0.02 + ... < 2.0
        self.assertLess(
            max_abs, 2.0,
            f"max |reward| = {max_abs:.3f} exceeds normalised-shaping budget"
        )

    def test_step_handles_out_of_range_actions(self):
        """Out-of-range actions (e.g. -1, 99) must be coerced to WAIT and
        flagged as invalid rather than crashing the env with IndexError.
        """
        env = _make_env(num_agents=3, horizon=8, seed=5)
        env.reset(seed=5)
        actions = {
            env.agents[0]: 99,           # past NUM_ACTIONS
            env.agents[1]: -1,           # negative
            env.agents[2]: WAIT_SLOT,    # baseline
        }
        # Must not raise.
        obs, rewards, _, _, _ = env.step(actions)
        for a in env.agents:
            self.assertTrue(np.isfinite(rewards[a]))
            self.assertIn("observation", obs[a])
        # The two out-of-range agents should pick up r_invalid_action; the
        # baseline agent should not. We compare against the WAIT-only agent
        # to stay robust to whatever per-agent shaping looks like.
        baseline_r = rewards[env.agents[2]]
        for bad_agent in (env.agents[0], env.agents[1]):
            self.assertLessEqual(
                rewards[bad_agent], baseline_r + 1e-6,
                f"{bad_agent} sent out-of-range action but reward "
                f"({rewards[bad_agent]:.4f}) is not penalised vs baseline "
                f"({baseline_r:.4f})"
            )

    def test_validator_conflict_flags_propagate_to_reward(self):
        """When the raw policy proposes a vertex collision the validator
        catches it, but BOTH agents should still receive the
        r_vertex_collision penalty so the policy gets a learning signal.
        """
        env = _make_env(num_agents=2, horizon=8, seed=7)
        env.reset(seed=7)

        # Find a node with >=2 predecessors so we can engineer a vertex conflict.
        target_node = None
        preds_pair = None
        for n in env._nodes:
            preds = list(env.G.predecessors(n))
            if len(preds) >= 2:
                target_node = n
                preds_pair = preds[:2]
                break
        if target_node is None:
            self.skipTest("Map has no node with 2+ predecessors")

        def slot_to(src, target):
            _, s2n = env.compass.get(src)
            for s, nbr in s2n.items():
                if nbr == target:
                    return s
            return None

        slot_a = slot_to(preds_pair[0], target_node)
        slot_b = slot_to(preds_pair[1], target_node)
        if slot_a is None or slot_b is None:
            self.skipTest("Compass collapsed both predecessors into the same slot")

        a0, a1 = env.agents[0], env.agents[1]
        # Strip tasks so reward is just r_step + (any) r_vertex_collision
        # + (any) r_forced_wait_by_validator. Avoids progress-shaping noise.
        for name, src in zip((a0, a1), preds_pair):
            info = env._agent_info[name]
            info.pos = src
            info.prev_pos = src
            info.task = None
            info.state = AgentState.IDLE
            info.age = 0.0
            info.age_idle = 0.0

        # Empty the dispatch pool so no fresh task gets assigned mid-step
        env._task_gen.pending.clear()
        env._task_gen.in_flight.clear()

        actions = {a0: int(slot_a), a1: int(slot_b)}
        _, rewards, _, _, infos = env.step(actions)

        self.assertGreaterEqual(infos[a0]["conflicts_vertex"], 1)
        # Both agents had vertex_flags set → both must dip below
        # r_step + r_vertex_collision (-0.01 + -1.0 = -1.01).
        for a in (a0, a1):
            self.assertLessEqual(
                rewards[a], env.cfg.r_step + env.cfg.r_vertex_collision + 1e-6,
                f"{a} reward {rewards[a]:.4f} missing r_vertex_collision "
                "(conflict flag wiring may be broken)"
            )


if __name__ == "__main__":
    unittest.main(verbosity=2)
