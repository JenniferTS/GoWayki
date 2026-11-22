# Baseline de rendimiento (entorno Linux del grupo)

Fecha: 2026-11-22  
Host: servidor compartido (sin dispositivo físico Android conectado para runtime).

## Artefactos

| Métrica | Valor | Notas |
|---|---|---|
| APK debug KMP | **~14 MB** | `androidApp-debug.apk` (incluye `pack.sqlite` ~16 MB comprimido en APK) |
| APK release Flutter (referencia) | ~110 MB | Offline MapLibre + tiles; no comparable 1:1 |
| Tests unitarios KMP | **37 / 37 OK** | Planner, money, reports, tutorial, sqlite loader |
| Cold start device | no medido | Sin emulador/dispositivo aquí |
| Tiempo hasta buscar ruta | no medido en UI | Red se carga en hilo `city-pack`; UI arranca sin bloquear |
| Frames / jank mapa | n/a | Mapa MapLibre aún no integrado; preview Compose |

## Presupuestos provisionales (a validar en gama media)

- Cold start → Home interactiva: < 2.5 s (sin cargar red completa)
- Primera búsqueda usable tras pack listo: < 4 s en frío
- Plan toy/network: ya < 400 ms en host (suite RAPTOR)
- APK release objetivo: < 40 MB sin tiles MBTiles; con mapa offline reevaluar

## Optimizaciones ya aplicadas

- Pack SQLite abierto en background; UI no espera
- Juego no inicia timers hasta entrar a la pantalla; `DisposableEffect` cancela
- `Money` sin Double; tarifa base centralizada
- Onboarding sin assets pesados (Canvas vectorial)

## Pendiente de medición

- Macrobenchmark / Baseline Profiles Android
- Instruments en iOS
- Lecturas Firestore y GPS duty cycle
