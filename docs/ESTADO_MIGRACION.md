# GoWayki KMP — estado de migración

Actualizado: 2026-11-22 (Baseline Profiles + iOS docs + emulador TCG)  
Rama: `feat/kmp-migration` → https://github.com/JenniferTS/GoWayki/tree/feat/kmp-migration  
Original Flutter intacto: `~/workspace/gowayki-lab-respaldo` @ `31fbe31`

## Matriz

| Funcionalidad | Original | KMP | Estado |
|---|---|---|---|
| Planificador RAPTOR | Sí | Sí + tests | Verificado |
| Búsqueda FTS5 | Sí | Android SQLite | Verificado build |
| Tarifa S/ 1.30 | Pack 1.0 | Money.BASE 130 | Verificado |
| Onboarding ES/EN | Sí | Sí + sync remoto | Implementado |
| Tutorial post-Google | — | Local + Firestore preferences | Implementado |
| Auth Google | Sí | Android real + demo | Implementado |
| Reportes Firestore | Sí | Cliente remoto + borrador local | Implementado |
| Feed reportes cercanos | Sí | Home + mapa | Implementado |
| Mapa MapLibre offline | Sí | SDK + mbtiles Arequipa | Implementado |
| Viaje activo / alarmas | Sí | TripTracker + GPS + notif | Implementado |
| Ranking / scores | Sí | Firestore leaderboards | Implementado |
| Juego | WebView | Compose + publish score | Implementado |
| Baseline Profiles | — | `baseline-prof.txt` + módulo `:baselineprofile` + ProfileInstaller | Implementado |
| Emulador en este host | — | TCG (`ANDROID_I_WANT_MY_TCG`) vía `tool/run-emulator-tcg.sh` | Usable (lento) |
| iOS | — | Shell Xcode + docs/IOS.md + permisos ubicación | Shell listo (build en Mac) |

## Cómo ejecutar Android

```bash
export ANDROID_HOME=/home/agentes/android-sdk
cd ~/workspace/gowayki-kmp
./gradlew :androidApp:assembleDebug
./tool/install-apk.sh
# sin KVM:
./tool/run-emulator-tcg.sh
```

## Baseline Profiles

```bash
# con device/emulator online:
./gradlew :baselineprofile:generateBaselineProfile
```

Ver `docs/RENDIMIENTO_BASELINE.md`, `docs/EMULADOR.md`, `docs/IOS.md`.

## Firebase

- Proyecto `gowayki-lab`, package `pe.edu.tecsup.gowayki_lab`
- SA: `~/.config/firebase-tokens/gowayki-lab-sa.json` (fuera de Git)
- Ranking: `leaderboards/{arequipa_YYYY-Www}/scores/{uid}`
