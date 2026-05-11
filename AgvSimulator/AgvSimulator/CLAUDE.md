# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Compile
mvn compile

# Package (fat jar)
mvn package

# Run directly via Maven (requires exec-maven-plugin or IDE)
# In practice, run Main.java from the IDE or:
mvn exec:java -Dexec.mainClass="cfg.aubot.itteam.simulator.Main"
```

Java 21 is required (set in `pom.xml` compiler plugin).

There are no tests in this project.

## Architecture Overview

This is a **software AGV (Automated Guided Vehicle) simulator** that mimics real vehicle behavior for testing a Fleet Management System (FMS). Vehicles communicate with the FMS over two different transport protocols.

### Core Class Hierarchy

```
VirtualAgv (abstract Thread)
├── VirtualMqttAgv   — VDA5050 v2.0.0 over MQTT
└── VirtualTCPAgv    — Proprietary binary protocol over TCP
```

`VirtualAgv` holds shared state: current position, operation state, battery, load state, working routes, and a `PositionSubscriber` callback used for collision detection across all AGVs.

### VirtualMqttAgv (primary implementation)

Implements the **VDA5050 v2.0.0** standard. Each instance is identified by a `name` (e.g. `V01`) and a `serialNumber` (same as name).

**MQTT topics** (prefix `aubotagv/2.0.0/AUBOT/<serialNumber>`):
- Subscribes: `.../order`, `.../instantActions`
- Publishes: `.../state` (every 200 ms at 5 Hz), `.../connection` (retained, with LWT)

**MQTT broker**: `tcp://localhost:1883`, credentials `fms` / `Aubot@2025`

**Order processing loop** (`processRequest`, runs every 100 ms):
1. Dequeues the first `Node` from `nodes` list.
2. Collision check — if destination is occupied by another AGV in the shared `agvPointMap`, it waits.
3. Traverses the corresponding `Edge`, incrementing `distance` in a tight loop (simulates motion at `edge.maxSpeed * 0.005` per 100 ms tick).
4. On arrival, fires `positionSubscriber.onPositionChange`, executes node-level `ActionState`s (`liftUp`, `liftDown`, `startCharging`, etc.), then removes the node.
5. Pause/resume is supported via `startPause`/`stopPause` instant actions.

**State publishing** uses a `Timer` at 200 ms intervals that serializes `State` (VDA5050 `State` object) to JSON via Jackson.

**Coordinate tracking**: `knownNodePositions` caches `NodePosition` (x, y) from orders so `agvPosition` can be interpolated during edge traversal (`updateAgvPositionAlongPath`).

### VirtualTCPAgv (legacy)

Opens a `ServerSocket` and speaks a custom binary protocol. Packet format: `0xFD | length(4) | type(1) | seqId(2) | payload | checksum | 0xFE`. Message types 1–7 correspond to state, order, error, moving, route, currentRoute, and setRoute requests.

### telegrams/vda5050/

Complete Java model of the VDA5050 v2.0.0 schema. Key classes:
- `Order` — received from FMS; contains `Node[]` and `Edge[]` sequences.
- `State` — published by the AGV; refreshed by `State.refresh(...)` each timer tick.
- `InstantActions` — side-channel commands (pause, cancel, charging, etc.).
- `Connection` — retained MQTT message indicating online/offline.
- `Factsheet` — static AGV capability document.

Jackson `ObjectMapper` is configured with `NON_NULL` serialization and lenient deserialization (`FAIL_ON_UNKNOWN_PROPERTIES = false`).

### telegrams/predefined/

Older proprietary MQTT telegram format (pre-VDA5050). `TelegramFactory` parses JSON into typed objects. Not used by the current `VirtualMqttAgv` implementation.

### AgvVirtualError

A Swing `JFrame` panel that lets a developer manually inject error flags into a running `VirtualMqttAgv` or `VirtualTCPAgv` via checkboxes. Attach it with `agv.setErrorManager(new AgvVirtualError())`.

### Main.java

Instantiates 12 `VirtualMqttAgv` instances (V01–V12) with hard-coded initial node IDs and a shared `agvPointMap` (`ConcurrentHashMap<VirtualAgv, String>`) used for collision detection. Additional vehicles V13–V25 are commented out.

## Key Conventions

- **Position IDs** are 4-character strings (e.g. `"0047"`, `"3027"`) matching node IDs in the FMS map.
- **Theta convention**: `theta = 0` points toward Y+; X+ direction is `+π/2`. This is non-standard and differs from typical ROS/VDA5050 convention where theta=0 is X+.
- **Distance** field (`distanceSinceLastNode`) ranges 0–1 during edge traversal (normalized, not meters).
- Lombok `@Data` / `@AllArgsConstructor` is used in data classes under `telegrams/predefined/` and `WorkingRoutes`. VDA5050 model classes use plain getters/setters with Jackson `@JsonProperty`.
- Logging is via Log4j2 (`LogManager.getLogger`), configured in `src/main/resources/log4j2.properties` (INFO level to console).
