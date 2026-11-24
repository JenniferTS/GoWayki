import java.util.Properties

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("com.google.gms.google-services")
    id("androidx.baselineprofile")
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    sourceSets {
        androidMain {
            dependencies {
                implementation(project(":shared"))
                implementation(project.dependencies.platform("com.google.firebase:firebase-bom:35.2.0"))
                implementation("com.google.firebase:firebase-auth")
                implementation("com.google.firebase:firebase-firestore")
                implementation("com.google.android.gms:play-services-location:21.4.0")
                implementation("org.maplibre.gl:android-sdk-opengl:13.7.1")
                implementation("androidx.profileinstaller:profileinstaller:1.4.1")
            }
        }
    }
}

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
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.myapplication"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        applicationId = "pe.edu.tecsup.gowayki_lab"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = 16
        versionName = "1.5.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"959294066022-7jl40t2irpfk5p9f2nn9j5b4og4es583.apps.googleusercontent.com\"",
        )
        // Páginas legales aún no publicadas: vacío oculta los enlaces en Ajustes > Acerca de.
        // Se fijan con -Pgowayki.privacyUrl=https://… / -Pgowayki.termsUrl=https://…
        buildConfigField("String", "PRIVACY_URL", "\"${findProperty("gowayki.privacyUrl") ?: ""}\"")
        buildConfigField("String", "TERMS_URL", "\"${findProperty("gowayki.termsUrl") ?: ""}\"")
    }
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    "baselineProfile"(project(":baselineprofile"))
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        check(releaseSigningReady) {
            "Release sin firma: falta ~/.gowayki-signing/key.properties o GOWAYKI_* (ver docs/RELEASE.md). Nunca firmar release con debug."
        }
    }
}
