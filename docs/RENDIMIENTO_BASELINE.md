# Baseline de rendimiento

Fecha: 2026-11-22  
Host: servidor compartido del grupo (VM sin KVM / sin vmx·svm).

## Artefactos actuales

| Métrica | Valor | Notas |
|---|---|---|
| APK debug KMP | ~65–70 MB | Incluye mbtiles Arequipa + pack SQLite |
| Tests unitarios | suite `:shared:testDebugUnitTest` | Planner, TripTracker, ranking week, money… |
| Baseline Profile | `androidApp/src/main/baseline-prof.txt` | Hand-authored + regenerable |
| ProfileInstaller | cableado en `GowaykiApp` | Escribe el perfil al primer arranque |
| Macrobenchmark | módulo `:baselineprofile` | Requiere device/emulator online |

## Generar perfiles en dispositivo

Con teléfono por USB o emulador ya booteado:

```bash
export ANDROID_HOME=/home/agentes/android-sdk
cd ~/workspace/gowayki-kmp
./gradlew :androidApp:assembleBenchmark
./gradlew :baselineprofile:generateBaselineProfile
# o medición de cold start:
./gradlew :baselineprofile:connectedBenchmarkAndroidTest
```

El generador abre la app, omite onboarding si aparece y toca la CTA de búsqueda.

## Emulador en este host (sin KVM)

Android Emulator 37 bloquea `-accel off` salvo TCG forzado:

```bash
chmod +x tool/run-emulator-tcg.sh tool/install-apk.sh
./tool/run-emulator-tcg.sh          # ANDROID_I_WANT_MY_TCG=yes, 1 GB RAM
./tool/install-apk.sh               # adb install APK debug
```

Es **muy lento** (TCG). Preferible:

```bash
adb connect <IP-del-teléfono>:5555
./tool/install-apk.sh
```

## Presupuestos

- Cold start → Home interactiva: < 2.5 s (sin esperar red completa)
- Primera búsqueda usable tras pack listo: < 4 s en frío
- Plan RAPTOR (tests host): ya < 400 ms en redes toy
- Release sin mbtiles: < 40 MB; con mapa offline el APK crece ~4 MB tiles + libs nativas MapLibre

## Optimizaciones aplicadas

- Pack SQLite en background
- Juego cancela timers al salir
- `Money` sin Double; tarifa base centralizada
- Baseline Profile + ProfileInstaller
- MapLibre assets instalados una vez a `filesDir`
