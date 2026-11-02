#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cat "$ROOT"/.pmf-source/part*.b64 | base64 --decode > "$ROOT/pmf-source.zip"
unzip -oq "$ROOT/pmf-source.zip" -d "$ROOT/.pmf-restored"
printf 'Restored PMF source to %s/.pmf-restored/PMF\n' "$ROOT"
