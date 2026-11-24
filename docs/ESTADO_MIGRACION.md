# GoWayki KMP — estado de migración

Actualizado: 2026-11-23 (identidad gowayki go)  
Rama: `feat/kmp-migration` → https://github.com/JenniferTS/GoWayki/tree/feat/kmp-migration  
Original Flutter intacto: `~/workspace/gowayki-lab-respaldo` @ `31fbe31`

## Matriz

| Funcionalidad | Estado |
|---|---|
| RAPTOR / FTS5 / tarifa S/ 1.30 | Verificado |
| Auth Google + tutorial sync | Implementado |
| Reportes Firestore + votos/confirmaciones | Implementado |
| MapLibre offline + paraderos/reportes/línea | Implementado |
| Rutas marcadas (casing + fit bounds + preview) | Implementado |
| Identidad gowayki go (`#F05709` / `#292929` + logo) | Implementado |
| Iconos UI lúdicos (stroke redondeado) | Implementado |
| Origen = mi ubicación | Implementado |
| Favoritos / viajes recientes | Implementado |
| Explorador de líneas | Implementado |
| Viaje activo + foreground service | Implementado |
| Ranking + cola offline de scores | Implementado |
| Baseline Profiles | Implementado |
| UI brand + botones pill con press/shimmer | Implementado |
| iOS shell | Docs + stubs (Mac) |
| Ilustraciones micros en APK (15 webp, onboarding + juego, decodificadas reducidas) | Implementado |
| i18n es/en completo (idioma del sistema por defecto, elegible y persistido) | Implementado |
| Tema por tokens + modo oscuro (UI y mapa) | Implementado |
| Arranque/sesión sin bloquear el hilo de UI (Firestore en segundo plano) | Implementado |
| R8 (minify + shrinkResources) en release/benchmark | Implementado |

## Versión

APK debug **1.5.6** (`versionCode` 16) — recorrido motion (JourneySheet, TuRuta/Luma). Anterior: 1.5.5 logo blanco (`versionCode` 15).

```bash
export ANDROID_HOME=/home/agentes/android-sdk
./gradlew :androidApp:assembleDebug
./tool/install-apk.sh
```
