# Brechas Flutter → KMP

Análisis: 2026-11-23, leyendo ambos repos (sin compilar). `F` = repo Flutter (`gowayki-lab-respaldo` @ `31fbe31`), `KS` = `kmp/shared/src`.

No hay Cloud Functions en ninguno de los dos proyectos: todo el backend es cliente + `firestore.rules`. Las rules desplegadas (`firebase/firestore.rules`) ya cubren feedback, `deletedAccounts`, `*Refs` y `users/{uid}/state`, así que casi todo lo de abajo **no necesita cambios de backend**.

## Prioridad

| # | Brecha | KMP hoy | Esfuerzo | Recomendación |
|---|---|---|---|---|
| 1 | **Borrar cuenta** | nada | M | **Ya.** Play exige borrar la cuenta desde la app si se pueden crear cuentas. Referencia: `F/lib/data/firebase/firebase_account_deletion_repository.dart`, `F/lib/app/sync/account_deletion_controller.dart`, `F/tool/firebase/erase-account.mjs`. |
| 2 | **Atribución del mapa (OSM/OpenMapTiles) + links de privacidad/términos** | atribución **apagada** (`KS/androidMain/.../ui/map/CityMap.android.kt:84-85`) | S | **Ya.** La licencia ODbL obliga. Flutter: `F/lib/app/settings/legal_links.dart`. |
| 3 | **Bug: cola offline de puntajes compartida entre cuentas** | `readPendingScores()` sin uid; `flushPendingScores()` publica toda la cola con el uid que entra; `signOutToGuest` no la limpia | S | **Ya.** Los puntos de una cuenta se publican en otra. Flutter guarda una cola por uid y no encola invitados (`F/lib/app/game/weekly_scores.dart`, test `F/test/weekly_account_test.dart`). |
| 4 | **Permisos de ubicación/notificaciones al arrancar** | `bootstrap()` → `warmLocation()` pide ubicación al abrir; POST_NOTIFICATIONS en `onCreate` | S | **Ya.** Play exige una pantalla de aviso antes de pedir la ubicación (sobre todo con foreground service de ubicación). Flutter: `F/lib/app/features/location/.../location_disclosure_screen.dart`. |
| 5 | Crashlytics / Analytics | nada | S–M | Antes de la release, mínimo Crashlytics. Copiar los defaults de privacidad de Flutter (`F/lib/infra/observability/`): apagado por defecto, solo en release, eventos enum, stack traces sin rutas ajenas a la app. |
| 6 | Pantalla de feedback | Ayuda es texto estático | S | Portar: barato y el backend ya está (`feedback/{id}` + `users/{uid}/feedbackRefs`). |
| 7 | Detalle de paradero, detalle de línea, interacción con el mapa | `LinesScreen` es solo lista; `CityMap` sin taps; paraderos sin id | M–L | Portar: es el núcleo de la app de transporte. |
| 8 | Biblioteca (casa, trabajo, lugares, paraderos, viajes guardados, líneas favoritas) | favoritos y recientes básicos, solo locales | M | Portar. |
| 9 | Sync en la nube de datos personales | solo tutorial y onboarding | M–L | Después de #8. |
| 10 | Notificaciones del viaje | un solo canal (modo "vibrar" igual suena), id aleatorio (se apilan), `cancelAll()` | S | Arreglar: un canal por modo, id fijo. |
| 11 | Compartir ruta / línea | nada | S | Portar (trivial con `Intent.ACTION_SEND`). |
| 12 | Juego | Compose placeholder (tap a círculos, sin vidas ni fin) | S–M | Opción A: el juego JS de Flutter (`F/assets/game/`) en WebView. Opción B: portar el motor (~45 líneas) a Kotlin común + Canvas (sirve también para iOS). |
| 13 | Actualización del paquete de datos de la ciudad por red | nada | M–L | Después: sirve solo cuando el pipeline de publicación de datos esté vivo (`F/hosting/` no existe, nunca se desplegó). |
| 14 | Ajustes: tema, distancia máxima a pie, evitar transbordos, versión de datos, "Acerca de" | parcial | S | Después. |
| 15 | "Mis reportes", reportes ligados a paradero/línea | `myReports` existe sin pantalla | S | Junto con #7. |
| 16 | Email/contraseña, invitado anónimo de Firebase con upgrade | solo Google | M | Decisión de producto. |
| 17 | Verificación de integridad (SHA-256) del paquete y del mapa | solo un stamp | S | Después. |
| 18 | `data/demo` | cubierto | — | Descartar. |
| 19 | Scripts `tool/` (pipeline Python OSM/GTFS → `pack.sqlite`) | no | — | No portar el pipeline: compartirlo. Sí portar los tests de rules (`F/tool/firebase/rules.test.mjs`), porque las rules de KMP cambiaron sin tests. |

`pack.sqlite` y `manifest.json` de KMP son idénticos byte a byte a los de Flutter. `routes.geojson` no se copió.

## Tests

Flutter: 34 archivos. KMP: 10, todos en `commonTest`, sin tests Android ni de UI Compose.

Prioridad para portar:
- `F/test/trip_tracker_test.dart` (10 casos vs 4 en KMP): GPS impreciso, viejo o desordenado, desvío de la ruta, cruce en bucle cerca del destino, zona de transbordo, recuperación después de perder GPS. **Son los casos de seguridad de la alarma de bajada.**
- `weekly_account_test` (cambio de cuenta → bug #3) y `community_test`.
- Un test JVM del planificador contra el `pack.sqlite` real (hoy solo hay fixtures).
- Algunos tests de UI con `compose-ui-test`: orden de permisos en onboarding y desbordes en pantallas chicas.
