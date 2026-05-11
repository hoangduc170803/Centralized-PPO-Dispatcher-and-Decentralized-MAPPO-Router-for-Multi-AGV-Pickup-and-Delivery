"""Compass-based action space mapper for irregular graph MAPF.

Action space (fixed size 9):
    [N, NE, E, SE, S, SW, W, NW, WAIT]

For each state, given current node and graph adjacency, computes:
  - action_mask: shape [9], 1 = valid action, 0 = invalid
  - slot_to_neighbor: dict {slot_index: target_node_id}

If two outgoing neighbors fall into the same compass bucket, the closer one
wins. This is rare for warehouse layouts (avg out-degree ≈ 3, max ≈ 3).

Angle convention: atan2(dy, dx). North = +y direction.
"""

from __future__ import annotations

import math
from typing import Mapping

import networkx as nx
import numpy as np


N_COMPASS_SLOTS = 8
WAIT_SLOT = 8
NUM_ACTIONS = 9

SLOT_NAMES = ["N", "NE", "E", "SE", "S", "SW", "W", "NW", "WAIT"]


def _angle_to_slot(angle_rad: float) -> int:
    """Map atan2 angle (East=0, North=+pi/2) to compass slot 0-7 (N=0, clockwise)."""
    return round((math.pi / 2 - angle_rad) / (math.pi / 4)) % 8


def compute_action_mask(
    G: nx.DiGraph, current: str
) -> tuple[np.ndarray, dict[int, str]]:
    """Compute action mask + slot-to-neighbor map for one agent at `current`.

    Returns:
        mask: np.ndarray shape [9], dtype int8. 1 = valid, 0 = invalid.
              mask[WAIT_SLOT] is always 1.
        slot_to_neighbor: dict mapping each valid slot (incl. WAIT) to
                          the target node id. WAIT maps to `current`.
    """
    mask = np.zeros(NUM_ACTIONS, dtype=np.int8)
    mask[WAIT_SLOT] = 1
    slot_to_neighbor: dict[int, str] = {WAIT_SLOT: current}

    cx = G.nodes[current]["x"]
    cy = G.nodes[current]["y"]

    candidates: dict[int, tuple[str, float]] = {}
    for nbr in G.successors(current):
        dx = G.nodes[nbr]["x"] - cx
        dy = G.nodes[nbr]["y"] - cy
        if dx == 0 and dy == 0:
            continue
        angle = math.atan2(dy, dx)
        slot = _angle_to_slot(angle)
        dist = math.hypot(dx, dy)
        if slot not in candidates or candidates[slot][1] > dist:
            candidates[slot] = (nbr, dist)

    for slot, (nbr, _) in candidates.items():
        mask[slot] = 1
        slot_to_neighbor[slot] = nbr

    return mask, slot_to_neighbor


class CompassMapper:
    """Cached compass mapper. Build once per graph, reuse across agents/timesteps."""

    def __init__(self, G: nx.DiGraph):
        self.G = G
        self._cache: dict[str, tuple[np.ndarray, dict[int, str]]] = {}

    def get(self, node: str) -> tuple[np.ndarray, dict[int, str]]:
        cached = self._cache.get(node)
        if cached is not None:
            return cached
        result = compute_action_mask(self.G, node)
        self._cache[node] = result
        return result

    def mask(self, node: str) -> np.ndarray:
        return self.get(node)[0]

    def resolve(self, node: str, slot: int) -> str:
        """Return next node id for given action slot. Returns `node` if WAIT or invalid."""
        _, slot_to_nbr = self.get(node)
        return slot_to_nbr.get(slot, node)

    def clear_cache(self):
        self._cache.clear()


if __name__ == "__main__":
    import sys
    from pathlib import Path

    repo_root = Path(__file__).resolve().parents[2]
    sys.path.insert(0, str(repo_root / "src"))
    from map_parser import parse_opentcs_map

    G = parse_opentcs_map(
        str(repo_root / "orca_share_media1778260607027_7458565577098821053.xml"),
        restrict_to_largest_scc=True,
    )
    mapper = CompassMapper(G)

    print("Sample nodes and their action masks:")
    for node in list(G.nodes())[:5]:
        mask, s2n = mapper.get(node)
        active = [(SLOT_NAMES[i], s2n[i]) for i in range(NUM_ACTIONS) if mask[i]]
        print(f"  {node}: mask={mask.tolist()}  active={active}")

    # Sanity: count active slots distribution
    from collections import Counter

    slot_counts = Counter()
    for node in G.nodes():
        mask = mapper.mask(node)
        slot_counts[int(mask.sum())] += 1
    print(f"\nDistribution of active slots per node: {dict(slot_counts)}")
