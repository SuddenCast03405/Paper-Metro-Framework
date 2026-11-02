# PMF — Paper Metro Framework
[![Build](https://github.com/SuddenCast03405/Paper-Metro-Framework/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/SuddenCast03405/Paper-Metro-Framework/actions/workflows/build.yml)

An original modular transit simulation framework for **Paper 1.21.8**. The server owns simulation and rendering; players do not need client-side mods. PMF does not depend on a hybrid server, TrainCarts or BKCommonLib.

## Current playable prototype
- `/pmf track kit` gives PMF-tagged standard and powered rail items. Their item identity is namespaced with the plugin's `pmf:rail` PersistentDataContainer key; the placed block remains a vanilla rail for client compatibility.
- PMF registers only rails placed from its tagged items. Ordinary vanilla rails, including existing player-built railways, are ignored and are not modified. `/pmf track list` prints PMF-owned route-node IDs.
- On upgrade from the earlier prototype, legacy auto-captured `rail:` graph records are removed from PMF's SQLite database; the physical vanilla rail blocks are never changed. Rebuild any intended PMF line with the PMF rail kit.
- Track connectivity still follows the placed vanilla rail's straight, corner and ascending block shape; PMF stores the network and route graph separately in SQLite.
- `/pmf train spawn <id> <start-node> <end-node>` automatically finds a shortest connected route through the placed rails.
- For manual control, `/pmf train spawn <id> <node1,node2,...>` still accepts an explicit node sequence.
- The train renderer now uses a three-car layered metro silhouette: silver body shells, roof, continuous dark window bands, red waist stripe, end glazing, underframes and separate bogies.
- Display entities are visual only. PMF therefore creates invisible server-side Slime collision proxies for each car and checks the consist's sampled volume against solid blocks, triggering a protective stop on obstruction.

## Requirements
- Java 21
- Gradle 8.x
- Paper 1.21.8

## Build
```bash
gradle clean test build
```
GitHub Actions builds and tests all modules, packages the shaded plugin JAR, then boots a real Paper 1.21.8 server to verify plugin enablement and SQLite initialization. The shaded plugin JAR is written to `pmf-plugin/build/libs/pmf-plugin-0.1.0-SNAPSHOT.jar`.

## Modules
- `pmf-api`: shared immutable value types and state enums.
- `pmf-engine`: track graph validation and fixed-step train kinematics; includes JUnit tests.
- `pmf-plugin`: Paper event integration, PMF-tagged rail placement capture, SQLite persistence, display consist and collision proxies.

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
This is an early implementation, not a finished DKZ5 model. The consist is built from scaled vanilla block displays, not a custom resource-pack mesh; the collision proxies are approximate, and block obstruction sampling is not a complete swept-volume physics engine. Rail topology follows vanilla straight, corner and ascending shapes; complex junctions, switches, crossings and full 3D curve interpolation are not yet implemented. Rail removal is synchronized with the graph and SQLite, and shortest-path routing plus basic bogie geometry are present. Still missing are true 3D curve interpolation, switches/interlocking, a detailed DKZ5 resource-pack mesh, physically accurate swept-volume collision, doors, passenger seating, stations, signalling, timetables, MySQL and schema migrations. Do not use this prototype for real-world safety-critical control.

All Bukkit world/entity access must remain on the server thread.
See [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/CONFIGURATION.md](docs/CONFIGURATION.md).
