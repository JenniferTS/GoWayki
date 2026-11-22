#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-/home/agentes/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
# Low-RAM headless for shared server
exec emulator -avd gowayki_api35 -no-window -no-audio -no-boot-anim \
  -gpu swiftshader_indirect -memory 1536 -cores 2 -no-snapshot-save "$@"
