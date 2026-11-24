# Plan de upgrade del stack (KMP Android)

Preparado: 2026-11-23. Versiones verificadas contra Maven Central / Google Maven ese día.

## Estado (2026-11-24)

**Etapas 1 y 2 hechas** en `chore/stack-upgrade` (un commit por etapa). La app ya cumple los dos requisitos de Play: `targetSdk 36` y librerías nativas alineadas a 16 KB. Falta la etapa 3.

Desvíos respecto del plan, con el porqué:

- **Compose Multiplatform 1.11.1, no 1.12.2.** CMP 1.12.x arrastra `androidx.compose.*` 1.12.1, cuyo `aar-metadata.properties` declara `minAndroidGradlePluginVersion=9.1.0` y `minCompileSdk=37` (`checkDebugAarMetadata` falla con AGP 8.13.2). 1.11.1 es la última que compila con AGP 8.x (`androidx.compose` 1.11.2: AGP ≥ 8.6, compileSdk ≥ 35). CMP 1.12.2 pasa a la etapa 3, junto con AGP 9.
- **compileSdk 36 ya en la etapa 1** (targetSdk siguió en 34 hasta la etapa 2): `androidx.compose` 1.11 y `core` 1.16 piden compileSdk ≥ 35.
- **`core-ktx` 1.18.0, no 1.19.x**: 1.19 también pide AGP ≥ 9.1 y compileSdk 37.
- **Sin `iosX64`**: Compose Multiplatform 1.11+ ya no publica ese target (simulador en Mac Intel). Quedan `iosArm64` e `iosSimulatorArm64`.
- **R8 y lint más nuevos que los de AGP 8.13**: el R8 y el lint embebidos en AGP 8.13.2 no leen metadata de Kotlin 2.4 (lint imprimía `e: … incompatible version of Kotlin … expected version is 2.2.0`; R8, `An error occurred when parsing kotlin metadata`). Se usa R8 9.5.33 en el classpath del `buildscript` raíz y `android.experimental.lint.version=9.5.2`. Quedan dos avisos inofensivos (`option … is experimental` y `R8: Class file resource provider does not support async parsing`). Las dos piezas se borran al pasar a AGP 9.5 (etapa 3).
- **`compose.components.resources` quitado**: el código no usa la API `Res`; logo, fuentes e ilustraciones se cargan por `R` (Android) desde `composeResources`, que sigue siendo la única copia. Desde CMP 1.6 el plugin además copia esa carpeta a `assets/composeResources` (~6 MB duplicados en el APK): las tareas `copy*ComposeResourcesToAndroidAssets` quedan apagadas en `shared/build.gradle.kts`.
- **Atrás del sistema**: antes no había ningún manejo (Atrás cerraba la app desde cualquier pantalla). Con targetSdk 36 el back predictivo viene activo, así que se agregó `SystemBackHandler` (expect/actual sobre `BackHandler` de activity-compose): cada pantalla con flecha atrás hace lo mismo con el gesto; Resultados → Buscar; Buscar → Inicio (la flecha de Buscar vuelve a Resultados, y con el gesto eso sería un ciclo). Inicio, Splash, Onboarding, Auth y el aviso de ubicación conservan el comportamiento del sistema (salir).

Tamaños de APK (universal, 4 ABI): debug 72,2 MB → 91,9 MB (dex sin minificar de Kotlin 2.4 / Compose 1.11); release firmado 61,3 MB → 60,4 MB.

## Por qué

| Problema hoy | Consecuencia |
|---|---|
| `targetSdk 34` | Google Play exige **API 36** para apps nuevas y updates desde el 31-08-2026: hoy no se puede subir. |
| MapLibre `11.5.2`: `libmaplibre.so` alineado a 4 KB (verificado leyendo los `LOAD p_align` del AAR) | Aviso de compatibilidad en Android 15+ con páginas de 16 KB; Play bloquea updates sin 16 KB desde el 01-02-2027. |
| Kotlin 1.9.21 / AGP 8.0.2 / Compose MP 1.5.11 / Gradle 8.2.1 | Dos años atrás; librerías nuevas (credentials 1.6, Firebase BoM 35) ya no compilan contra esto. |

## Versiones objetivo

| Componente | Antes | Objetivo | Usado (etapas 1-2) |
|---|---|---|---|
| Kotlin (KGP) | 1.9.21 | **2.4.21** | 2.4.21 |
| Compose Multiplatform | 1.5.11 | **1.12.2** (+ plugin `org.jetbrains.kotlin.plugin.compose` 2.4.21, misma versión que Kotlin) | **1.11.1** (1.12 pide AGP ≥ 9.1, ver Estado) + plugin compose 2.4.21 |
| AGP | 8.0.2 | Etapa 1: **8.13.2** · Etapa 3: **9.5.2** | 8.13.2 |
| Gradle | 8.2.1 | Etapa 1: **8.14.8** · Etapa 3: **9.9.2** (AGP 9.5 pide ≥ 9.6.0) | 8.14.8 |
| compileSdk / targetSdk | 34 / 34 | **36 / 36** (minSdk 24 se mantiene; MapLibre 12+ pide ≥ 23) | 36 / 36 (compileSdk 37 en la etapa 3, para Compose 1.12) |
| MapLibre | `android-sdk:11.5.2` | **`android-sdk-opengl:13.7.1`** (alineado a 16 KB, verificado) | 13.7.1 opengl |
| Firebase BoM | 33.7.0 | **35.2.0** | 35.2.0 (auth 25.0.0, firestore 27.2.0; el código ya no usaba artefactos `-ktx`) |
| androidx.credentials | 1.3.0 | **1.6.0** | 1.6.0 |
| googleid | 1.1.1 | **1.2.1** | 1.2.1 |
| play-services-location | 21.3.0 | **21.4.0** | 21.4.0 |
| kotlinx-coroutines | 1.7.3 | **1.11.0** | 1.11.0 |
| activity-compose | 1.7.2 | **1.13.0** | 1.13.0 |
| appcompat / core-ktx / profileinstaller | 1.6.1 / 1.10.1 / 1.3.1 | estables compatibles con SDK 36 | 1.8.0 / 1.18.0 / 1.4.1 |
| androidx.test (baselineprofile) | junit 1.1.5, espresso 3.5.1, uiautomator 2.3.0, benchmark 1.2.4 | — | 1.3.0, 3.7.0, 2.4.1, 1.5.0 |
| google-services plugin | 4.4.2 | **4.6.0** | 4.6.0 |
| baselineprofile plugin | 1.2.4 | **1.5.0** | 1.5.0 |
| foojay-resolver | 0.4.0 | compatible con Gradle 8.14 | 1.0.0 |

## Cambios incompatibles a tener en cuenta

**MapLibre 11 → 13**
- 13.0.0: `org.maplibre.gl:android-sdk` pasó a renderizar con **Vulkan**. Para no cambiar el comportamiento usar `android-sdk-opengl` (Vulkan se puede evaluar después).
- 12.0.0: minSdk 21 → 23 (no nos afecta, estamos en 24).
- 11.6.0 / 11.7.1: cambios en custom layers (`currentLayer` → `index`, se quitó `Pass3D`). El proyecto no usa custom layers (verificado), no afecta.
- MBTiles/offline: el changelog no lista cambios incompatibles; igual probar el mapa offline de Arequipa a mano.

**Kotlin 2.x / Compose MP 1.12**
- Compose compiler ahora es plugin de Kotlin: aplicar `org.jetbrains.kotlin.plugin.compose` con la misma versión que KGP y quitar cualquier `kotlinCompilerExtensionVersion`.
- K2: posibles errores nuevos de compilación en `expect/actual` (hoy hay warnings de "expect/actual classes are in Beta"; agregar `-Xexpect-actual-classes`).
- `compose.components.resources`: la API de recursos cambió entre 1.5 y 1.6+ (accesores generados `Res.drawable.x` + `painterResource(Res.drawable.x)`). Hoy las fuentes y el logo se cargan por `expect/actual`, pero si se suman imágenes en `composeResources` (ilustraciones de combis), revisar esos usos.
- `compose.material` (M2) sigue disponible pero está en mantenimiento; migrar a Material 3 es una tarea aparte, no parte de este upgrade.
- `kotlin.mpp.androidSourceSetLayoutVersion=2` y `kotlin.mpp.applyDefaultHierarchyTemplate=false` en `gradle.properties`: el layout v2 es el único en Kotlin 2.x; quitar las flags y usar la jerarquía por defecto (el `iosMain by creating` manual se puede borrar).

**AGP 9 (etapa 3)** — lo más grande:
- **KMP + `com.android.application` en el mismo módulo no está soportado.** `androidApp` tiene que pasar a ser un módulo Android puro: plugin `com.android.application` solo (Kotlin viene integrado en AGP 9, no se aplica `org.jetbrains.kotlin.android`), sources de `src/androidMain/kotlin` → `src/main/kotlin`, manifest a `src/main/AndroidManifest.xml`, dependencias en el bloque `dependencies {}` normal.
- `shared`: `com.android.library` → **`com.android.kotlin.multiplatform.library`**. El bloque `android {}` se mueve a `kotlin { android { namespace; compileSdk; minSdk } }`. Este plugin **no tiene build types ni BuildConfig** (hoy `shared` no los usa: el `GOOGLE_WEB_CLIENT_ID` está en `androidApp`, bien). Recursos Android apagados por defecto: `androidResources { enable = true }` (hay fuentes en `src/androidMain/res/font`). Tests JVM: `withHostTest {}`; las carpetas pasan a `androidHostTest`.
- `android.newDsl=false` / `android.builtInKotlin=false` existen para salir del paso, pero el opt-out se elimina en AGP 10: no usarlos salvo de puente.
- `baselineprofile`: módulo `com.android.test`, revisar que el plugin 1.5.0 funcione con AGP 9.5.

**Síntoma que se fue con la etapa 1** (R8 9.5.33 + lint 9.5.2, ver Estado): `assembleRelease` imprimía ~25 líneas `e: … Module was compiled with an incompatible version of Kotlin … expected version is 1.7.1` desde `:androidApp:lintVitalAnalyzeRelease`. Es el lint embebido de AGP 8.0.2 que no lee metadata de Kotlin 1.9; el build termina bien y el APK no se ve afectado.

## Orden de ejecución (una rama, un commit por etapa, build + tests verdes en cada una)

1. ✅ **Toolchain sin reestructurar.** Gradle 8.14.8, AGP 8.13.2, Kotlin 2.4.21 + plugin compose, Compose MP 1.12.2, limpiar flags de `gradle.properties`. Arreglar errores de K2 y la API de recursos. Verificar: `./gradlew :shared:testDebugUnitTest :androidApp:assembleDebug` + smoke en emulador.
2. ✅ **SDK y librerías.** compileSdk/targetSdk 36 (cambios de comportamiento de Android 15/16: edge-to-edge forzado en targetSdk 35+ → insets en todas las pantallas; el foreground service del viaje activo ya declara `foregroundServiceType="location"`), MapLibre 13.7.1 opengl, Firebase BoM 35.2.0, credentials 1.6.0, googleid 1.2.1, resto de la tabla. Verificar en emulador: mapa offline, viaje activo con notificación, login Google, reportes Firestore, y que **no** aparezca el diálogo de 16 KB.
3. **AGP 9.5.2 + Gradle 9.9.2 + reestructuración** de `androidApp` y `shared` (ver arriba). Verificar lo mismo que en la etapa 2 + `assembleBenchmark`. Sumar lo que quedó pendiente por AGP 8: Compose Multiplatform 1.12.2 con compileSdk 37, `core-ktx` 1.19.x, y quitar el R8 del `buildscript` raíz y `android.experimental.lint.version` de `gradle.properties`.

Las etapas 1 y 2 ya dejan la app subible a Play. La etapa 3 se puede hacer después, pero antes de AGP 10.

## Riesgo principal

**Edge-to-edge (etapa 2, hecho)**: `enableEdgeToEdge()` en `MainActivity` (y otra vez en `onConfigurationChanged`, porque `uiMode` está en `configChanges` y los íconos de las barras tienen que seguir al modo oscuro). `GowaykiScreen` usa `safeDrawingPadding()`; la barra inferior de Inicio y el panel de Resultados se extienden detrás de la barra de navegación con el contenido encima; los mapas de Inicio y Resultados se dibujan detrás de la barra de estado; los velos de los `Popup` (tutorial, borrar cuenta) se dibujan en la ventana principal porque la ventana del `Popup` no cubre las barras. Revisado en el emulador (Android 16) en claro y oscuro. Texto original: con targetSdk 35+, Android dibuja la app detrás de las barras del sistema, y hoy el código **no maneja ningún inset** (no hay `WindowInsets` ni `systemBarsPadding` en todo el proyecto, verificado). Todas las pantallas necesitan `WindowInsets` (`systemBarsPadding()` / `navigationBarsPadding()`, o `Scaffold` con `contentWindowInsets`). Es justo el tipo de bug que ya apareció (contenido tapado por la barra inferior), así que hay que revisar todas las pantallas en el emulador.
