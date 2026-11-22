#!/usr/bin/env bash
# Usage: FIREBASE_TOKEN=1//xxxx ./tool/use-firebase-token.sh projects:list
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
: "${FIREBASE_TOKEN:?Pega el token de: firebase login:ci}"
exec node tool/firebase/node_modules/firebase-tools/lib/bin/firebase.js --project gowayki-lab "$@"
