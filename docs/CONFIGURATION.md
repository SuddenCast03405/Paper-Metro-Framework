# Configuration
- `simulation.tick-period`: simulation interval in server ticks; keep at 1 initially.
- `simulation.max-speed-blocks-per-second`: maximum speed in blocks per second.
- `simulation.acceleration`: blocks/s².
- `simulation.braking`: blocks/s².
- `render.enabled`: enable the proof-of-life renderer.
- PMF rail items use the `pmf:rail` plugin namespace marker stored in item persistent data; placed blocks remain vanilla rail blocks for client compatibility.
- Only rails placed from PMF-tagged items are registered in the PMF graph. Unmarked vanilla rails are ignored; PMF does not rewrite their block data or take ownership of their networks.
- `safety.stop-on-unknown-signal`: intended fail-safe default for the future signal module.
- `safety.require-connected-route`: intended route validation policy.
Current route format is a sequence of connected nodes. There is no block occupancy, station stop marker, switch interlocking or signal enforcement yet.
