# GoWayki iOS (shell KMP)

El framework Compose Multiplatform vive en `:shared` (`iosArm64` / `iosSimulatorArm64` / `iosX64`).  
**Este servidor es Linux:** no hay Xcode; el binario iOS se genera en una Mac.

## Requisitos (Mac)

- macOS + Xcode 15+
- CocoaPods opcional (Firebase iOS aún no cableado; Auth/Map son stubs)
- JDK 17 para Gradle

## Abrir y correr

```bash
cd ~/workspace/gowayki-kmp
./gradlew :shared:embedAndSignAppleFrameworkForXcode
open iosApp/iosApp.xcodeproj
```

En Xcode: target **iosApp**, equipo de firma de sharon, Run en simulador o iPhone.

`ContentView.swift` embebe `Main_iosKt.MainViewController()` (Compose).

## Qué funciona hoy en iOS

| Área | Estado |
|---|---|
| UI Compose (onboarding, búsqueda demo, juego, ranking UI) | Sí (framework) |
| City pack SQLite / RAPTOR | Necesita driver iOS SQLite (pendiente cablear) |
| MapLibre offline | Preview Canvas (sin SDK nativo aún) |
| Auth Google / Firestore | Stubs (`platformRemoteReports` / ranking = null) |
| Viaje activo GPS | Stub (sin CoreLocation aún) |

## Permisos

`Info.plist` declara ubicación cuando se use (ver claves `NSLocation*`).  
Firebase: `GoogleService-Info.plist` ya está en `iosApp/iosApp/`.

## Próximo cableado útil (Mac)

1. SQLDelight / sqlite-driver nativo para el city pack  
2. MapLibre iOS + mismos assets `map/`  
3. Firebase iOS Auth + Firestore con el mismo proyecto `gowayki-lab`
