# PMF infrastructure architecture

## Identity boundary

PMF component IDs use the pmf: namespace. They are application-level identifiers, not registrations of new Minecraft block IDs. The Paper adapter may use a vanilla block as a visual/physical backing while PMF owns the component identity, metadata and transit behavior.

## Component types

The API defines TrackComponentType values:

- RAIL: a PMF-owned track segment.
- SWITCH: a PMF-owned route selector / turnout.
- SIGNAL: a PMF-owned railway signal.
- STATION: a station-level infrastructure object.
- PLATFORM: a boarding platform associated with a station.
- DEPOT: a storage, inspection or dispatch facility.

TrackComponent stores a stable PMF component ID, type, world and position. TrackComponentRegistry provides in-memory registration, lookup, removal and type queries. Conflicting component IDs are rejected; registering the same component twice is idempotent.

## Persistence and vanilla compatibility

The Paper plugin persists component metadata in SQLite's track_components table. PMF-tagged rail items place vanilla rail blocks for compatibility with unmodified clients; only blocks placed from tagged PMF items are adopted into the PMF graph. Unmarked vanilla rails are not automatically scanned, rewritten or claimed by PMF.

The current implementation persists RAIL components. Other component types are API/engine foundations only; placement tools, block displays, route interlocking and signal logic will be implemented in later phases.

## Upgrade behavior

Legacy prototype nodes with the old rail: ID prefix are removed from PMF graph metadata during startup migration. Their physical world blocks are untouched. Players must explicitly rebuild intended PMF-owned track with the PMF rail kit.