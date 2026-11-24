# GoWayki (Kotlin Multiplatform)

Migración de la app Flutter `gowayki_lab` a **Kotlin Multiplatform + Compose Multiplatform** (Android e iOS).  
El original permanece intacto en `~/workspace/gowayki-lab-respaldo` (`31fbe31`).

## Estado

Ver `docs/ESTADO_MIGRACION.md`. Rama de trabajo: `feat/kmp-migration`.

## Requisitos

- JDK 17+ (Gradle 8.14.8; el build compila con toolchain 17)
- Android SDK (`ANDROID_HOME`, p. ej. `/home/agentes/android-sdk`) con platform 36 (compileSdk/targetSdk 36) y build-tools 36
- iOS: macOS + Xcode (no validado en este servidor Linux)

## Compilar / probar

```bash
export ANDROID_HOME=/home/agentes/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
cd ~/workspace/gowayki-kmp
./gradlew :shared:testDebugUnitTest
./gradlew :androidApp:assembleDebug
```

APK debug: `androidApp/build/outputs/apk/debug/`.

Instalar (teléfono o emulador TCG):

```bash
./tool/install-apk.sh
# sin KVM en este host:
./tool/run-emulator-tcg.sh
```

iOS (solo Mac + Xcode): ver `docs/IOS.md`.  
Rendimiento / Baseline Profiles: `docs/RENDIMIENTO_BASELINE.md`.  
Emulador sin KVM: `docs/EMULADOR.md`.

## Módulos

- `shared` — dominio (planificador RAPTOR, dinero, reportes, preferencias), datos SQLite, UI Compose común
- `androidApp` — Activity, assets city pack + mapa offline, ProfileInstaller
- `baselineprofile` — Macrobenchmark / generador de Baseline Profiles
- `iosApp` — shell Xcode (framework `shared`)
- `firebase/` — rules/indexes locales (proyecto `gowayki-lab`); **no desplegar sin autorización**
## Tarifa

Pasaje base centralizado: `Money.BASE` = **130** minor units **PEN** (S/ 1.30).  
Las tarifas por línea del pack tienen precedencia; el `default_fare` 1.0 del SQLite se ignora a propósito.

## Identidad visual

Paleta zorro andino (terracota `#B9532F`, arena, marfil, carbón). Microinteracciones ~150 ms; transiciones ~240 ms; respeta movimiento reducido.

## Firebase

Package Android: `pe.edu.tecsup.gowayki_lab`.  
Auth Google real y sync remoto: pendiente de cablear SDK KMP y login CLI en este entorno.
