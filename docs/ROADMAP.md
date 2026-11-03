# PMF development roadmap

## Phase 0 — foundation (implemented)
- Multi-module Java 21 / Paper 1.21.8 build.
- API value types, SQLite node/edge persistence and connected track graph.
- Rail placement/removal capture and topology derived from vanilla straight, corner and ascending rail shapes.
- Automatic shortest-path route lookup.
- Fixed-step kinematic polyline train simulation.
- Three-car layered display renderer, basic bogie geometry and approximate collision proxies.
- Admin commands and JUnit coverage for graph routing and train kinematics.

## Phase 1 — track engineering (in progress)
- Arc-length parameterized spline segments (Catmull-Rom / cubic Bézier) for smooth curves.
- Logical three-node switch model with mutually exclusive straight/diverging route selection in the engine.
- Switch objects, route locking and conflict matrix in the Paper runtime.
- Track editor wand, GUI and import/export.
- Chunk/world availability and rail topology reconciliation on restart.
- Dedicated tests for slopes, curves, junctions and rail-shape changes.
Acceptance: trains traverse straight, curved, sloped and switched track smoothly without route ambiguity.

## Phase 2 — train consists and DKZ5 models
- Replace the block-display silhouette with a detailed resource-pack model inspired by the DKZ5 profile.
- Car-specific end cabs, windshields, doors, destination signs, underframe equipment and bogies.
- Couplers and per-car articulation on curves.
- Viewer lifecycle and client interpolation.
- Passenger seat anchors.
Acceptance: multi-car model follows curved track with visually articulated cars and passengers can ride safely.

## Phase 3 — collision and stations
- Swept-volume collision detection against blocks and entities.
- Station/platform/stop-marker records.
- Braking curve and stop tolerance.
- Dwell timer and door-side mapping.
- Door state machine, platform screen-door animation and obstruction interlocks.
Acceptance: correct platform stop, synchronized doors and safe departure interlock.

## Phase 4 — signalling
- Block occupancy spans for the full train length.
- Reservations and route locking.
- Red/yellow/green aspects and speed supervision.
- Fail-safe state on missing data, unloaded worlds and restart recovery.
Acceptance: automated multi-train stress tests show no conflicting block reservations.

## Phase 5 — dispatch and passenger information
- Fixed-headway and fixed-timetable services.
- Depot fleet pool and train assignment.
- Delay propagation and turnback.
- Destination/next-stop displays and localized audio.
- Optional NPC passenger boarding/alighting.
Acceptance: a service plan runs unattended for several in-game hours.

## Phase 6 — web operations
- Read-only HTTP JSON API.
- SVG schematic line map and train positions.
- Station status and delay display.
- Authentication/audit log for write endpoints.
- Optional BlueMap marker adapter.
Acceptance: web map reflects server state without blocking the tick thread.

## Phase 7 — production hardening
- Repository interfaces for SQLite and MySQL.
- Schema migrations and crash-safe snapshots.
- Reconciliation on restart.
- Metrics, profiling, backups and compatibility matrix.
