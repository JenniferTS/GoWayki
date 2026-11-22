# GoWayki KMP — estado de migración

Actualizado: 2026-11-22 (noche)  
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
| Rules tipos nuevos | — | Desplegadas en prod | Verificado API |
| Ubicación puntual reportes | Sí | Permiso + lastKnown | Implementado |
| Mapa MapLibre offline | Sí | Preview Compose | Parcial |
| Viaje activo / alarmas | Sí | — | Ausente |
| Ranking / scores | Sí | — | Ausente |
| Juego | WebView | Compose | Implementado |
| iOS | — | Shell Xcode | No prioridad |
| Baseline Profiles | — | — | Pendiente |
| Emulador Android (KVM) | — | AVD sin KVM | Bloqueado host |

## Cómo ejecutar Android

```bash
export ANDROID_HOME=/home/agentes/android-sdk
cd ~/workspace/gowayki-kmp
./gradlew :androidApp:assembleDebug
# APK: androidApp/build/outputs/apk/debug/
```

## Firebase

- Proyecto `gowayki-lab`, package `pe.edu.tecsup.gowayki_lab`
- SA: `~/.config/firebase-tokens/gowayki-lab-sa.json` (fuera de Git)
- Rules prod con `heavy_traffic` / `road_blocked` (deploy sharon 2026-11-22)
