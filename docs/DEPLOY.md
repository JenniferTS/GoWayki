# Despliegue y ejecución — gowayki go (KMP)

Documento de entrega CPS / proyecto integrador.  
Repo: https://github.com/JenniferTS/GoWayki · rama de trabajo: `feat/kmp-migration`

## 1. Qué se despliega

| Componente | Dónde corre | Notas |
|---|---|---|
| App Android (Compose Multiplatform) | Dispositivo / emulador | `applicationId` `pe.edu.tecsup.gowayki_lab` |
| Datos de ciudad (GTFS / pack) | SQLite embebido en el APK | `assets/city/arequipa/pack.sqlite` |
| Mapa | MapLibre offline | `arequipa.mbtiles` + `style.json` |
| Auth / comunidad / ranking | Firebase proyecto `gowayki-lab` | Auth + Firestore |
| APK para distribución | Servidor Linux con nginx (Docker) | Ver sección 7 |

No hay un backend REST propio que haya que levantar en un VPS para el MVP móvil: el planificador (RAPTOR) corre **en el dispositivo**.

## 2. Requisitos

- JDK **17+**
- Android Studio Hedgehog+ (o solo Android SDK 34–36)
- `ANDROID_HOME` apuntando al SDK
- Cuenta Firebase con `google-services.json` (ya versionado en `androidApp/`)
- Para release: keystore en `~/.gowayki-signing/key.properties` (ver `docs/RELEASE.md`)

## 3. Compilar (debug)

```bash
export ANDROID_HOME=/ruta/al/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
cd GoWayki   # raíz del repo (settings.gradle.kts)

./gradlew :shared:testDebugUnitTest
./gradlew :androidApp:assembleDebug
```

APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

Instalar:

```bash
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
# o
./tool/install-apk.sh
```

## 4. Ejecutar en Android Studio

1. File → Open → carpeta raíz del repo (no solo `androidApp`).
2. Sync Gradle.
3. Seleccionar configuración **androidApp**.
4. Emulador API 34+ o teléfono con depuración USB.
5. Run ▶.

Deep links de widget/shortcuts: `gowayki://search?origin=&dest=`

## 5. Firebase

- Proyecto: **gowayki-lab**
- Package Android: `pe.edu.tecsup.gowayki_lab`
- SHA-1 debug/release deben estar registrados (ver `docs/FIREBASE_ACCESO.md`)
- Rules locales en `firebase/`; **no desplegar rules sin autorización del equipo**

Emuladores Firebase (opcional, desarrollo):

```bash
./tool/start-firebase-emulators.sh
```

## 6. “Servidor” / entorno de demo en la VM del grupo

Para demos sin teléfono físico:

```bash
export ANDROID_I_WANT_MY_TCG=yes   # si no hay KVM
./tool/run-emulator-tcg.sh
./tool/install-apk.sh
```

El emulador TCG es lento; se recomienda dispositivo real para la demo móvil.

## 7. Despliegue en un servidor

La app no necesita un servidor propio para funcionar. Lo que sí se despliega en servidores es el APK (para distribuirlo) y las reglas de Firebase.

| Pieza | Dónde | Cómo |
|---|---|---|
| APK para descarga | Servidor Linux con Docker | Compilar en el servidor y publicar con nginx (pasos abajo) |
| Reglas e índices de Firestore | Firebase `gowayki-lab` | `firebase deploy --project gowayki-lab --only firestore:rules,firestore:indexes` |

### 7.1 Compilar en el servidor

Requisitos: Git, JDK 17 y Android SDK (plataforma 36 y build-tools 36).

```bash
git clone https://github.com/JenniferTS/GoWayki.git
cd GoWayki
git checkout feat/kmp-migration
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME

./gradlew :shared:testDebugUnitTest :androidApp:assembleDebug
```

### 7.2 Publicar el APK con nginx (Docker)

```bash
sudo mkdir -p /srv/gowayki
sudo cp androidApp/build/outputs/apk/debug/androidApp-debug.apk /srv/gowayki/

docker run -d --name gowayki-apk --restart unless-stopped \
  -p 8080:80 -v /srv/gowayki:/usr/share/nginx/html:ro nginx:alpine
```

Descarga desde el teléfono: `http://<IP-del-servidor>:8080/androidApp-debug.apk`.
Si el servidor se expone a Internet, ponerlo detrás de un proxy inverso con HTTPS.

Para actualizar el APK, copiar el archivo nuevo a `/srv/gowayki/`. Para retirarlo: `docker rm -f gowayki-apk`.

### 7.3 Reglas e índices de Firebase

Las reglas están en `firebase/firestore.rules` y los índices en `firebase/firestore.indexes.json`. **No desplegar sin autorización del equipo.**

```bash
# desde un equipo con Node.js; el token se obtiene con: firebase login:ci
cd tool/firebase && npm install && cd ../..
FIREBASE_TOKEN=<token> ./tool/use-firebase-token.sh deploy --only firestore:rules,firestore:indexes
```

En Windows también sirve `.\tool\deploy-firestore.ps1` con `firebase login`.

### 7.4 Release firmado

La firma de release y la publicación en Play se describen en `docs/RELEASE.md`.

## 8. Artefactos relacionados (ecosistema)

| Pieza | Repo / ruta | Uso en videos |
|---|---|---|
| App móvil KMP | este repo | Perfil **móvil** |
| Paradero Digital (kiosco/admin) | repo cloud-iot / rama paradero | Perfil **admin** / **usuario kiosco** |
| IoT | `JenniferTS/gowayki-iot` | Opcional en admin |

## 9. Checklist de aceptación del despliegue

- [ ] `assembleDebug` OK
- [ ] App abre splash → onboarding o home
- [ ] Búsqueda O–D devuelve planes (pack presente)
- [ ] Mapa offline visible
- [ ] Login Google o modo invitado
- [ ] Reportes / favoritos no crashean sin red (cola offline donde aplique)
- [ ] El APK se descarga desde el servidor (`curl -I http://<IP>:8080/androidApp-debug.apk` devuelve 200)

## 10. Contacto del equipo

Sharon Trujillo · Piero Gómez · Leonardo Monserrate · Fabricio Rojas · Avril Enriquez  
Curso: Construcción y Pruebas de Software — proyecto integrador GoWayki.
