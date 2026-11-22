# Emuladores en este servidor

## Firebase Auth + Firestore (recomendado aquí)
```bash
cd ~/workspace/gowayki-kmp
./tool/start-firebase-emulators.sh
```
UI: http://127.0.0.1:4000 — Auth :9099 — Firestore :8080

## Android Emulator
- AVD: `gowayki_api35` (API 35, Google APIs x86_64)
- Script: `./tool/start-emulator.sh`
- **Limitación:** este VPS **no tiene /dev/kvm**. El emulador software es muy lento o inestable con ~2–3 GB libres.
- Alternativas para que el agente pruebe la APK:
  1. Conectar un teléfono por `adb connect IP:5555` (depuración Wi‑Fi).
  2. Usar otra máquina del grupo con KVM y exponer adb.
  3. Seguir con emuladores Firebase + tests JVM mientras tanto.

## APK actual
`./gradlew :androidApp:assembleDebug`
