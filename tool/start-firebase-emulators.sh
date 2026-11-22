#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export FIREBASE_PROJECT=gowayki-lab
exec node tool/firebase/node_modules/firebase-tools/lib/bin/firebase.js emulators:start \
  --project gowayki-lab --only auth,firestore
