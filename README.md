# PMF — Paper Metro Framework

[![Build](https://github.com/SuddenCast03405/Paper-Metro-Framework/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/SuddenCast03405/Paper-Metro-Framework/actions/workflows/build.yml)

PMF is an original modular transit simulation framework for Paper 1.21.8. It does not require a hybrid server, client-side mods, TrainCarts or BKCommonLib.

## Project source

The initial 19-file starter project is preserved as a compressed source archive encoded in `.pmf-source/` to keep the first Git import atomic and lossless. Restore the regular Gradle project tree with:

```bash
bash scripts/restore-source.sh
cd .pmf-restored/PMF
gradle clean build
```

Requirements: JDK 21 and Gradle 8.10.2. The GitHub Actions workflow restores the archive, runs the same Gradle build on every push to `main` and pull request, and uploads the plugin JAR as a build artifact.

## Included starter implementation

- Gradle multi-project: `pmf-api`, `pmf-engine`, `pmf-plugin`
- Paper 1.21.8 plugin descriptor and configuration
- SQLite persistence for track nodes and bidirectional edges
- Track graph and connected-route validation
- Acceleration/braking simulation along connected polyline nodes
- Basic BlockDisplay renderer and `/pmf` administration commands

This is an early prototype, not a production railway system. Curves, switches, multiple carriages, passenger handling, block signalling/interlocking, stations, platform screen doors, timetables, MySQL, and the custom resource-pack model pipeline remain roadmap work.

See `docs/ROADMAP.md` and `docs/CONFIGURATION.md` after restoring the project source.

## Safety and threading

All Bukkit world/entity access stays on the server thread. Do not operate this prototype as a production passenger railway: signalling, collision protection, interlocking, and crash-recovery behavior are not yet implemented or stress-tested.
