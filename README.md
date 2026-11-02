# PMF — Paper Metro Framework
[![Build](https://github.com/SuddenCast03405/Paper-Metro-Framework/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/SuddenCast03405/Paper-Metro-Framework/actions/workflows/build.yml)

An original modular transit simulation framework for **Paper 1.21.8**. The server owns simulation and rendering; players do not need client-side mods. PMF does not depend on a hybrid server, TrainCarts or BKCommonLib.

## Current playable prototype
- Place vanilla rails (normal, powered, detector or activator rails) directly in the world. PMF records each placed rail as a track node and links nearby rail nodes in SQLite.
- `/pmf track kit` gives a starter bundle; `/pmf track list` prints the generated route-node IDs.
- `/pmf train spawn <id> <start-node> <end-node>` automatically finds a shortest connected route through the placed rails.
- For manual control, `/pmf train spawn <id> <node1,node2,...>` still accepts an explicit node sequence.
- The train renderer now uses a three-car, layered light-grey/red/black-glass display consist instead of a single iron block.
- Display entities are visual only. PMF therefore creates invisible server-side Slime collision proxies for each car and checks the consist's sampled volume against solid blocks, triggering a protective stop on obstruction.

## Requirements
- Java 21
- Gradle 8.x
- Paper 1.21.8

## Build
```bash
gradle clean test build
```
GitHub Actions runs the build and unit tests on pushes, pull requests and manual dispatch. The shaded plugin JAR is written to `pmf-plugin/build/libs/pmf-plugin-0.1.0-SNAPSHOT.jar`.

## Modules
- `pmf-api`: shared immutable value types and state enums.
- `pmf-engine`: track graph validation and fixed-step train kinematics; includes JUnit tests.
- `pmf-plugin`: Paper event integration, rail placement capture, SQLite persistence, display consist and collision proxies.

## Commands
- `/pmf help`
- `/pmf track kit`
- `/pmf track list`
- `/pmf node add <id> [speed]`
- `/pmf node connect <from> <to>`
- `/pmf node list`
- `/pmf train spawn <id> <start-node> <end-node>`
- `/pmf train spawn <id> <node1,node2,...>` (explicit route)
- `/pmf train stop <id>`
- `/pmf train list`

Permission: `pmf.admin` (default: operator).

## Known limitations
This is an early implementation, not a finished DKZ5 model. The consist is built from scaled vanilla block displays, not a custom resource-pack mesh; the collision proxies are approximate, and block obstruction sampling is not a complete swept-volume physics engine. Track connections are inferred from nearby rail nodes and do not yet interpret all vanilla rail shapes, slopes, switches or crossings. Rail breaking/removal synchronization, automatic pathfinding, curves, bogies, doors, passenger seating, stations, signalling, timetables, MySQL and schema migrations remain future work. Do not use this prototype for real-world safety-critical control.

All Bukkit world/entity access must remain on the server thread.
See [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/CONFIGURATION.md](docs/CONFIGURATION.md).
