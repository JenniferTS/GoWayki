#!/usr/bin/env bash
# Instala el APK debug en el primer device/emulator adb online.
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-/home/agentes/android-sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="${1:-$ROOT/androidApp/build/outputs/apk/debug/androidApp-debug.apk}"

if [[ ! -f "$APK" ]]; then
  echo "No está el APK. Compilando…"
  (cd "$ROOT" && ./gradlew :androidApp:assembleDebug)
fi

adb wait-for-device
adb install -r "$APK"
echo "Instalado: $APK"
adb shell monkey -p pe.edu.tecsup.gowayki_lab -c android.intent.category.LAUNCHER 1 || true
