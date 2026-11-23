# GoWayki KMP — estado de migración

Actualizado: 2026-11-23 (rediseño UI + motion)  
Rama: `feat/kmp-migration` → https://github.com/JenniferTS/GoWayki/tree/feat/kmp-migration  
Original Flutter intacto: `~/workspace/gowayki-lab-respaldo` @ `31fbe31`

## Matriz

| Funcionalidad | Estado |
|---|---|
| RAPTOR / FTS5 / tarifa S/ 1.30 | Verificado |
| Auth Google + tutorial sync | Implementado |
| Reportes Firestore + votos/confirmaciones | Implementado |
| MapLibre offline + paraderos/reportes/línea | Implementado |
| Origen = mi ubicación | Implementado |
| Favoritos / viajes recientes | Implementado |
| Explorador de líneas | Implementado |
| Viaje activo + foreground service | Implementado |
| Ranking + cola offline de scores | Implementado |
| Baseline Profiles | Implementado |
| UI brand + animaciones (Atmosphere/Enter/Glass) | Implementado |
| iOS shell | Docs + stubs (Mac) |
| Ilustraciones micros en APK | **No** (a propósito, peso) |

## Versión

APK debug **1.3.0** (`versionCode` 6).

```bash
export ANDROID_HOME=/home/agentes/android-sdk
./gradlew :androidApp:assembleDebug
./tool/install-apk.sh
```
