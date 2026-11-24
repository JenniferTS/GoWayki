# Release Android (firma, Firebase, Play)

Preparado: 2026-11-23. El bloque de firma todavía **no está aplicado** en `androidApp/build.gradle.kts`.

## Keystore de subida (upload key)

Decidido 2026-11-23: **keystore nuevo**, no se reusa el `gowayki-upload.p12` que generó el proyecto Flutter en la PC de Nico (la app nunca se publicó, así que no se pierde nada).

| | |
|---|---|
| Archivo | `C:\Users\User\.gowayki-signing\gowayki-upload.jks` (PKCS12, RSA 4096, vence 2054) |
| Alias | `gowayki-upload` |
| Contraseñas | en `C:\Users\User\.gowayki-signing\key.properties` (aleatorias, nunca en git ni chat) |
| SHA-1 | `4C:2B:52:D0:A6:CD:68:B4:74:11:51:DA:33:8B:31:0A:5A:F2:EB:26` |
| SHA-256 | `7A:A8:5C:F9:2C:7F:24:94:6B:E5:75:C5:92:A6:7B:4F:63:D1:01:14:5D:23:7C:28:F3:81:F0:34:7F:8C:6D:8E` |

Mismo esquema que `~/.selfia-signing/`. **Respaldo**: vault de Ragnar, empresa GoWayki, entrada «Android release keystore (upload key)» (categoría infra). Tiene el `.jks` en base64, la contraseña, alias, huellas e instrucciones para restaurarlo; verificado que el base64 reconstruye el archivo idéntico. Si se pierde la upload key se puede resetear en Play Console, pero lleva días.

El SHA-1 viejo de Nico (`14:89:4E:…`) puede quedar en Firebase sin problema, o borrarse.

## Bloque de firma para `androidApp/build.gradle.kts`

Lee `~/.gowayki-signing/key.properties` (o la ruta de `GOWAYKI_SIGNING_PROPERTIES`) y, si no está, variables de entorno `GOWAYKI_*` (para CI). Si falta algo, `release` falla en vez de salir firmado con debug.

```kotlin
import java.util.Properties

val signingProps = Properties().apply {
    val path = System.getenv("GOWAYKI_SIGNING_PROPERTIES")
        ?: "${System.getProperty("user.home")}/.gowayki-signing/key.properties"
    val f = file(path)
    if (f.isFile) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    signingProps.getProperty(key)?.takeIf { it.isNotBlank() } ?: System.getenv(env)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "GOWAYKI_STORE_FILE")?.let { file(it) }
val releaseStorePassword = signingValue("storePassword", "GOWAYKI_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "GOWAYKI_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "GOWAYKI_KEY_PASSWORD")
val releaseSigningReady = releaseStoreFile?.isFile == true &&
    listOf(releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null }

android {
    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        check(releaseSigningReady) {
            "Release sin firma: falta keystore.properties o GOWAYKI_* (ver docs/RELEASE.md). Nunca firmar release con debug."
        }
    }
}
```

Formato de `key.properties` (ya creado en esta PC):

```properties
storePassword=...
keyPassword=...
keyAlias=gowayki-upload
storeFile=C:/Users/User/.gowayki-signing/gowayki-upload.jks
```

## Firebase / Google Sign-In en release

Google Sign-In valida el SHA-1 del APK que corre en el teléfono. En `gowayki-lab` tienen que estar:

| Firma | Para qué | Estado |
|---|---|---|
| debug de cada PC de desarrollo | builds locales | esta PC: `1F:A2:52:…:AC:07` ✅ registrado |
| upload key | APK/AAB release instalado a mano | `4C:2B:52:D0:A6:CD:68:B4:74:11:51:DA:33:8B:31:0A:5A:F2:EB:26` ❌ **falta registrar** |
| **app signing key de Play** | builds que instala Play Store | ❌ se obtiene en Play Console → Integridad de la app → Firma de apps, **después** de la primera subida |

Si falta la tercera, el login con Google funciona en todo menos en la app bajada de Play (`DEVELOPER_ERROR` / "Revisa SHA-1"). Error clásico.

## Publicar

1. Subir `versionCode` / `versionName` en `androidApp/build.gradle.kts`.
2. `./gradlew :androidApp:bundleRelease` → `androidApp/build/outputs/bundle/release/androidApp-release.aab`.
   - AAB y no APK: Play entrega a cada teléfono solo su ABI, y `libmaplibre.so` pesa ~11 MB por ABI (×4 en el APK universal).
3. Requisitos de Play: `targetSdk 36` y librerías nativas alineadas a 16 KB **se cumplen** desde 2026-11-24 (`docs/PLAN_UPGRADE_STACK.md`, etapas 1-2). Para comprobarlo en un APK: `zipalign -c -P 16 -v 4 <apk>` (build-tools 36) y que el emulador con páginas de 16 KB no muestre el diálogo «Android App Compatibility» en una instalación limpia.
4. Play Console: Play App Signing activado (default), subir a prueba interna primero, registrar el SHA-1 de app signing en Firebase, probar login desde la app bajada de Play.

## CI

`.github/workflows/android-ci.yml`: en cada push a `main` / `feat/**` y en PRs corre `:shared:testDebugUnitTest :androidApp:assembleDebug` y deja el APK debug como artifact (7 días). No firma release ni publica: eso queda manual hasta decidir dónde viven los secretos (`GOWAYKI_*` como secrets del repo, keystore en base64).

El repo es público, así que los minutos de Actions en runners estándar no se cobran. Para subir el workflow, la cuenta que hace push necesita el scope `workflow` (`gh auth refresh -s workflow`).
