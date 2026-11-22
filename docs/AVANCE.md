# Registro breve de avance

## 2026-11-22

- Inspección: no había KMP previo; Flutter completo en respaldo `31fbe31`.
- Creado `gowayki-kmp` en `feat/kmp-migration`.
- Port dominio: planner, money 1.30, city pack SQLite Android, reportes, tutorial policy.
- UI Compose: splash, onboarding, home, search/results/detail, reports, auth demo, profile/settings/help, game, tutorial overlay.
- Verificado: `./gradlew :shared:testDebugUnitTest` (37 OK) + `:androidApp:assembleDebug` (~14 MB).
- Firebase: rules locales con tipos nuevos; sin deploy; MCP sin autenticar.
- Siguiente: MapLibre, Google Sign-In real, sync tutorial/reportes, iOS en Mac, mediciones en dispositivo.

## 2026-11-22 (noche)
- Rules Firestore en producción con heavy_traffic/road_blocked (deploy de sharon).
- Auth Google Android + SHA debug registrados.
