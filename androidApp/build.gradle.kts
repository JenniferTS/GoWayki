plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("com.google.gms.google-services")
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
                implementation("com.google.firebase:firebase-auth")
                implementation("com.google.firebase:firebase-firestore")
                implementation("com.google.android.gms:play-services-auth:21.2.0")
                implementation("com.google.android.gms:play-services-location:21.3.0")
                implementation("org.maplibre.gl:android-sdk:11.5.2")
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.myapplication"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        applicationId = "pe.edu.tecsup.gowayki_lab"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = 3
        versionName = "1.1.0"
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"959294066022-7jl40t2irpfk5p9f2nn9j5b4og4es583.apps.googleusercontent.com\"",
        )
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}
