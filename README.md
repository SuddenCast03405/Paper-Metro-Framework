# PMF — Paper Metro Framework
[![Build](https://github.com/SuddenCast03405/Paper-Metro-Framework/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/SuddenCast03405/Paper-Metro-Framework/actions/workflows/build.yml)

An original modular transit simulation framework for **Paper 1.21.8**. The server owns simulation and rendering; players do not need client-side mods. PMF does not depend on a hybrid server, TrainCarts or BKCommonLib.

## Current playable prototype
- `/pmf track kit` gives PMF-tagged standard and powered rail items. Their item identity is namespaced with the plugin's `pmf:rail` PersistentDataContainer key; the placed block remains a vanilla rail for client compatibility.
- PMF registers only rails placed from its tagged items. Ordinary vanilla rails, including existing player-built railways, are ignored and are not modified. `/pmf track list` prints PMF-owned route-node IDs.
- On upgrade from the earlier prototype, legacy auto-captured `rail:` graph records are removed from PMF's SQLite database; the physical vanilla rail blocks are never changed. Rebuild any intended PMF line with the PMF rail kit.
- Track connectivity follows placed vanilla rail shapes. PMF switch definitions are logical three-node turnouts persisted in SQLite; the selected branch is honored by automatic route finding.
- `/pmf train spawn <id> <start-node> <end-node>` automatically finds a shortest connected route through the placed rails.
- For manual control, `/pmf train spawn <id> <node1,node2,...>` still accepts an explicit node sequence.
- The train renderer now uses a three-car layered metro silhouette: silver body shells, roof, continuous dark window bands, red waist stripe, end glazing, underframes and separate bogies.
- Display entities are visual only. PMF therefore creates invisible server-side Slime collision proxies for each car and checks the consist's sampled volume against solid blocks, triggering a protective stop on obstruction.

## PMF infrastructure namespace
PMF component IDs use the pmf: namespace and are independent of Minecraft block IDs. The engine has a typed registry for rails, switches, signals, stations, platforms and depots. The Paper adapter currently persists PMF rail components in SQLite; the other component types are defined for subsequent feature work and are not yet placeable in-game. A PMF rail may be backed by a vanilla rail block, while an ordinary vanilla rail is not automatically adopted.

## Logical switches (engine + command prototype)
- `/pmf switch create <id> <stem-node> <straight-node> <diverging-node>` creates a logical turnout over three already-connected nodes, initially set straight.
- `/pmf switch set <id> straight|diverging` changes the selected branch and persists it in SQLite.
- `/pmf switch list` shows each turnout and selected branch.
- The engine rejects invalid turnout topology and automatic/explicit route validation honors the selected branch. An active train whose route includes the switch nodes holds a conservative route lock until it finishes.
- Switches currently do not alter vanilla rail block shapes and have no in-world lever, display animation, automatic interlocking or signal integration yet.

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
- `pmf-api`: shared immutable value types, PMF infrastructure component types (RAIL, SWITCH, SIGNAL, STATION, PLATFORM, DEPOT) and state enums.
- `pmf-engine`: track graph validation, typed component registry and fixed-step train kinematics; includes JUnit tests.
- `pmf-plugin`: Paper event integration, PMF-tagged rail placement capture, SQLite persistence, display consist and collision proxies.

## Commands
- `/pmf help`
- `/pmf track kit`
- `/pmf track list`
- `/pmf switch create <id> <stem-node> <straight-node> <diverging-node>`
- `/pmf switch set <id> straight|diverging`
- `/pmf switch list`
- `/pmf node add <id> [speed]`
- `/pmf node connect <from> <to>`
- `/pmf node list`
- `/pmf train spawn <id> <start-node> <end-node>`
- `/pmf train spawn <id> <node1,node2,...>` (explicit route)
- `/pmf train stop <id>`
- `/pmf train list`

Permission: `pmf.admin` (default: operator).

## Known limitations
This is an early implementation, not a finished DKZ5 model. The consist is built from scaled vanilla block displays, not a custom resource-pack mesh; the collision proxies are approximate, and block obstruction sampling is not a complete swept-volume physics engine. Rail topology follows vanilla straight, corner and ascending shapes. Logical switches and route selection are implemented in the engine and command prototype, but do not physically animate or reshape vanilla rails; crossings and full 3D curve interpolation are not yet implemented. Rail removal is synchronized with the graph and SQLite, and shortest-path routing plus basic bogie geometry are present. Still missing are true 3D curve interpolation, full automatic interlocking and signal integration, a detailed DKZ5 resource-pack mesh, physically accurate swept-volume collision, doors, passenger seating, stations, signalling, timetables, MySQL and schema migrations. Do not use this prototype for real-world safety-critical control.

All Bukkit world/entity access must remain on the server thread.
See [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/CONFIGURATION.md](docs/CONFIGURATION.md).
