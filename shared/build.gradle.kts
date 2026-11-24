plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
}

val composeVersion = findProperty("compose.version") as String

kotlin {
    jvmToolchain(17)
    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
                implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
                implementation("org.jetbrains.compose.material:material:$composeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        androidMain {
            dependencies {
                api("androidx.activity:activity-compose:1.13.0")
                api("androidx.appcompat:appcompat:1.8.0")
                api("androidx.core:core-ktx:1.18.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
                implementation(project.dependencies.platform("com.google.firebase:firebase-bom:35.2.0"))
                implementation("com.google.firebase:firebase-auth")
                implementation("com.google.firebase:firebase-firestore")
                implementation("androidx.credentials:credentials:1.6.0")
                implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
                implementation("com.google.android.libraries.identity.googleid:googleid:1.2.1")
                implementation("com.google.android.gms:play-services-location:21.4.0")
                implementation("org.maplibre.gl:android-sdk-opengl:13.7.1")
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.myapplication.common"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    // Recursos Android = composeResources (logo, fuentes, ilustraciones de micros): una sola copia.
    sourceSets["main"].res.srcDirs("src/commonMain/composeResources")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// composeResources ya entra al APK como recursos Android (R.drawable / R.font, srcDirs de arriba). Desde
// Compose Multiplatform 1.6 el plugin además lo copia a assets/composeResources (~6 MB duplicados) aunque
// el código no use la API Res; se apaga esa copia mientras las fuentes e ilustraciones se carguen por R.
tasks.matching { it.name.endsWith("ComposeResourcesToAndroidAssets") }.configureEach { enabled = false }
