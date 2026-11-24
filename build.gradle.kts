buildscript {
    repositories {
        google()
    }
    dependencies {
        // R8 de AGP 8.13 no lee la metadata de Kotlin 2.4 (avisa en minifyReleaseWithR8); este sí.
        classpath("com.android.tools:r8:9.5.33")
    }
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("multiplatform").apply(false)
    kotlin("android").apply(false)
    kotlin("plugin.compose").apply(false)
    id("com.android.application").apply(false)
    id("com.android.library").apply(false)
    id("com.android.test").apply(false)
    id("org.jetbrains.compose").apply(false)
    id("androidx.baselineprofile").apply(false)
    id("com.google.gms.google-services").version("4.6.0").apply(false)
}
