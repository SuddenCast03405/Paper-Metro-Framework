# PMF development roadmap
## Phase 0 — included foundation
Multi-module build, API value types, SQLite node/edge persistence, track graph, connected-route validation, kinematic polyline train simulation, basic BlockDisplay renderer, admin commands.
## Phase 1 — track engineering
- Cubic Bézier / Catmull-Rom segments and arc-length lookup tables.
- Switch objects, route locking, conflict matrix.
- Track editor wand, GUI, import/export.
- World/chunk validation.
Acceptance: trains traverse straight, curved, sloped and switched track smoothly.
## Phase 2 — train consists and models
- Train consist, cars, couplers, bogies and per-car offsets.
- BlockDisplay/ItemDisplay model-part registry.
- Resource-pack model definitions and custom item/font support.
- Client interpolation and viewer lifecycle.
- Passenger seat anchors.
Acceptance: multi-car model follows the track and passengers can ride safely.
## Phase 3 — stations
- Station/platform/stop-marker records.
- Braking curve and stop tolerance.
- Dwell timer and door-side mapping.
- Door state machine, screen-door animation, obstruction checks.
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
