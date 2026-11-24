# Brechas Flutter → KMP

Análisis: 2026-11-23, leyendo ambos repos (sin compilar). `F` = repo Flutter (`gowayki-lab-respaldo` @ `31fbe31`), `KS` = `kmp/shared/src`.

No hay Cloud Functions en ninguno de los dos proyectos: todo el backend es cliente + `firestore.rules`. Las rules desplegadas (`firebase/firestore.rules`) ya cubren feedback, `deletedAccounts`, `*Refs` y `users/{uid}/state`, así que casi todo lo de abajo **no necesita cambios de backend**.

## Prioridad

| # | Brecha | KMP hoy | Esfuerzo | Recomendación |
|---|---|---|---|---|
| 1 | **Borrar cuenta** | ✅ hecho (2026-11-24, `fix/play-compliance`) | M | Perfil → "Eliminar cuenta" con diálogo (qué se borra, irreversible, progreso, errores es/en). Reverificación con Google (Credential Manager), pausa persistida de subidas, borrado de feedback/confirmaciones (devuelve el contador)/reportes/puntajes/`users/{uid}/…`, marca `deletedAccounts`, borra `users/{uid}` y el usuario de Auth, limpia la cola local de esa cuenta y vuelve a invitado. Reanudable: si se corta queda pendiente y Perfil ofrece completarlo. `domain/account/AccountDeletion.kt` + `data/account/FirebaseAccountDeletionRepository.kt` (iOS: null). |
| 2 | **Atribución del mapa (OSM/OpenMapTiles) + links de privacidad/términos** | ✅ hecho (2026-11-24) | S | Atribución visible en todo mapa (abajo a la izquierda, sobre la hoja en Resultados), toca → enlaces a OSM copyright y OpenMapTiles. Ajustes → "Acerca de" con texto ODbL y enlaces (`platformOpenUrl`). Privacidad/términos: `-Pgowayki.privacyUrl` / `-Pgowayki.termsUrl` (vacíos → ocultos, como `PRIVACY_URL`/`TERMS_URL` de Flutter). |
| 3 | **Bug: cola offline de puntajes compartida entre cuentas** | ✅ hecho (2026-11-24) | S | `PendingScoreQueue`: una cola por uid, invitados no encolan, mejor puntaje por semana, un cambio de cuenta a mitad de subida no publica nada en la cuenta nueva, cuenta en borrado en pausa. La cola compartida vieja (`pending_scores`) se **descarta** al arrancar: no se sabe de qué cuenta es. Tests en `PendingScoreQueueTest`. |
| 4 | **Permisos de ubicación/notificaciones al arrancar** | ✅ hecho (2026-11-24) | S | Nada se pide al abrir. Aviso destacado (`LocationDisclosureScreen`) tras la bienvenida y ante la primera acción explícita (mi ubicación, origen = mi ubicación, ubicación del reporte, iniciar viaje); solo su botón abre el diálogo del sistema. Aceptación persistida; usos pasivos nunca piden. POST_NOTIFICATIONS solo al iniciar un viaje activo. Sin permiso se planifica con origen manual. |
| 5 | Crashlytics / Analytics | nada | S–M | Antes de la release, mínimo Crashlytics. Copiar los defaults de privacidad de Flutter (`F/lib/infra/observability/`): apagado por defecto, solo en release, eventos enum, stack traces sin rutas ajenas a la app. |
| 6 | Pantalla de feedback | Ayuda es texto estático | S | Portar: barato y el backend ya está (`feedback/{id}` + `users/{uid}/feedbackRefs`). |
| 7 | Detalle de paradero, detalle de línea, interacción con el mapa | `LinesScreen` es solo lista; `CityMap` sin taps; paraderos sin id | M–L | Portar: es el núcleo de la app de transporte. |
| 8 | Biblioteca (casa, trabajo, lugares, paraderos, viajes guardados, líneas favoritas) | favoritos y recientes básicos, solo locales | M | Portar. |
| 9 | Sync en la nube de datos personales | solo tutorial y onboarding | M–L | Después de #8. |
| 10 | Notificaciones del viaje | un solo canal (modo "vibrar" igual suena), id aleatorio (se apilan), `cancelAll()` | S | Arreglar: un canal por modo, id fijo. |
| 11 | Compartir ruta / línea | nada | S | Portar (trivial con `Intent.ACTION_SEND`). |
| 12 | Juego | ✅ rehecho en Compose (2026-11-24): pista horizontal de 3 carriles, baches desde la derecha, deslizar/tocar para cambiar de carril, un bache termina la partida, "Empezar" reinicia; motor puro `CombiRunEngine` con tests | — | Hecho. Opción B (motor en Kotlin común + Canvas) |
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
