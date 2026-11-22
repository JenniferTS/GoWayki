#!/usr/bin/env bash
# Arranca el AVD gowayki_api35 en este host sin KVM (TCG software).
# Es lento; sirve para smoke install/adb. Preferible un teléfono por USB/Wi‑Fi.
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-/home/agentes/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
export ANDROID_I_WANT_MY_TCG=yes

AVD="${1:-gowayki_api35}"
LOG="${EMU_LOG:-/tmp/emu-gowayki.log}"

if adb devices | grep -qE 'emulator-[0-9]+\s+device'; then
  echo "Ya hay un emulador online."
  adb devices -l
  exit 0
fi

pkill -f "emulator @$AVD" 2>/dev/null || true
sleep 1

echo "Iniciando $AVD con TCG (sin KVM)…"
nohup "$ANDROID_HOME/emulator/emulator" "@$AVD" \
  -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect \
  -accel off -memory 1024 -cores 1 -no-snapshot \
  >"$LOG" 2>&1 &
echo "PID $!  log: $LOG"

echo "Esperando boot (puede tardar varios minutos)…"
adb wait-for-device
# boot completed
for i in $(seq 1 120); do
  boot=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
  if [[ "$boot" == "1" ]]; then
    echo "Boot OK ($i)."
    adb devices -l
    exit 0
  fi
  sleep 5
done
echo "Timeout esperando boot. Ver $LOG" >&2
exit 1
