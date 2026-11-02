# PMF — Paper Metro Framework
[![Build](https://github.com/SuddenCast03405/Paper-Metro-Framework/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/SuddenCast03405/Paper-Metro-Framework/actions/workflows/build.yml)
An original modular transit simulation framework for **Paper 1.21.8**. The server owns all simulation and rendering; players do not need client-side mods. PMF does not depend on a hybrid server, TrainCarts or BKCommonLib.
## Requirements
- Java 21
- Gradle 8.10.2 or compatible Gradle 8.x
- Paper 1.21.8
## Build
```bash
gradle clean test build
```
GitHub Actions runs the same build and unit tests on pushes, pull requests and manual dispatch. The shaded plugin JAR is written to `pmf-plugin/build/libs/pmf-plugin-0.1.0-SNAPSHOT.jar`.
## Install and first test
1. Copy the shaded JAR into the Paper server's `plugins/` directory and start the server.
2. Join as an operator. At the first point, run `/pmf node add A`.
3. Move roughly 20 blocks along the intended route and run `/pmf node add B`.
4. Run `/pmf node connect A B`.
5. Run `/pmf train spawn T001 A,B` and then `/pmf train list`.
The current renderer uses a scaled iron `BlockDisplay` as a proof-of-life train body, not a finished multi-car model.
## Modules
- `pmf-api`: shared immutable value types and state enums.
- `pmf-engine`: track graph validation and fixed-step train kinematics; includes JUnit tests.
- `pmf-plugin`: Paper entry point, administrative commands, SQLite persistence and display-entity renderer.
## Commands
- `/pmf help`
- `/pmf node add <id> [speed]`
- `/pmf node connect <from> <to>`
- `/pmf node list`
- `/pmf train spawn <id> <node1,node2,...>`
- `/pmf train stop <id>`
- `/pmf train list`
Permission: `pmf.admin` (default: operator).
## Current scope and limitations
Included: multi-module Gradle build, Paper plugin descriptor/configuration, SQLite storage for track nodes and bidirectional edges, connected-route validation, polyline train acceleration/braking, and a basic `BlockDisplay` renderer.
Not implemented yet: spline/curved track geometry, switches and interlocking, multi-car consists/couplers/seats, passenger mounting, stations/platforms, dwell and door logic, platform screen doors, block occupancy/signalling, timetables/dispatch, passenger information/audio, web operations map, MySQL adapter and schema migrations. These remain roadmap items, not working features.
See [`docs/ROADMAP.md`](docs/ROADMAP.md) and [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md). This prototype is not production-ready railway software: do not use it for real-world safety-critical control. All Bukkit world/entity access must remain on the server thread.
