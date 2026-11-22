# GoWayki KMP — estado de migración

Actualizado: 2026-11-22  
Rama: `feat/kmp-migration`  
Original (Flutter, intacto): `~/workspace/gowayki-lab-respaldo` → `NICO-bestacclaimed/gowayki-lab-respaldo-31fbe31` @ `31fbe31`

## Matriz funcional

| Funcionalidad | Original Flutter | Estado KMP | Pendiente | Prueba |
|---|---|---|---|---|
| Planificador RAPTOR offline | Implementado | Implementado y verificado (toy network) | Validar red real Arequipa en dispositivo | Unit + device |
| Búsqueda origen/destino FTS5 | Implementado | Implementado (Android SQLite) | iOS driver | Device |
| Tarifa base S/ 1.30 | Pack tenía S/ 1.00 | Implementado (`Money.BASE=130`) | Regenerar pack opcional | Unit |
| Onboarding 3 pasos ES/EN | Implementado (otro copy) | Implementado | Animaciones pulidas en device | UI manual |
| Tutorial contextual post-Google | Ausente / parcial | Implementado (política migración) | Sync Firebase real | Unit + device |
| Reportes comunidad | detour/missing_stop/… | Parcial (nuevos tipos + local) | Firebase remoto, geocerca, rate limit | Unit local; rules no desplegadas |
| Auth Google | Implementado | Parcial (demo + hooks) | GitLive/oficial KMP + SHA | Device |
| Mapa MapLibre offline | Implementado | Parcial (preview Compose) | MapLibre Android/iOS | Device |
| Viaje activo / alarmas | Implementado | Ausente | Port | — |
| Juego Wayki Combi | WebView/JS | Implementado Compose ligero | Paridad visual sprites | Unit/UI |
| Ranking semanal | Implementado | Ausente | Port | — |
| Actualización city pack | Implementado | Parcial (stamp SHA) | Hosting download | — |
| i18n ES/EN | Completo | Parcial (strings críticas) | Cobertura total ARB | — |
| Baseline Profiles / perf | Medido Windows AOT | Ausente | Release + Macrobenchmark | — |
| iOS runtime | No compilado en Windows | Config Xcode template | Build en macOS | Bloqueado aquí |

## Decisiones

- No se reescribió el repo Flutter; KMP vive en carpeta nueva.
- Tarifa: el loader ignora `default_fare.amount=1.0` del pack y usa `Money.BASE` (130 PEN). `line_fares` tiene precedencia.
- Reportes: tipos nuevos `heavy_traffic`, `detour`, `road_blocked`, `other`; legacy solo lectura. Caducidad por tipo en app; rules Firestore listas localmente (72h confirm window legacy).
- Tutorial: cuentas sin campo ⇒ completado (no “nuevo”).
- Firebase MCP en esta sesión: sin login; no se desplegó nada.
- iOS: sin macOS/Xcode en el servidor; framework KMP preparado, no validado.

## Cómo ejecutar

```bash
export ANDROID_HOME=/home/agentes/android-sdk
export ANDROID_SDK_ROOT=/home/agentes/android-sdk
cd ~/workspace/gowayki-kmp
./gradlew :shared:testDebugUnitTest
./gradlew :androidApp:assembleDebug
# APK: androidApp/build/outputs/apk/debug/
```

iOS (macOS): abrir `iosApp/iosApp.xcodeproj`, compilar framework `:shared`, correr en simulador.

## Firebase

- Proyecto: `gowayki-lab`
- Package Android: `pe.edu.tecsup.gowayki_lab`
- Rules locales: `firebase/firestore.rules` (incluye tipos nuevos; **no desplegado**)
