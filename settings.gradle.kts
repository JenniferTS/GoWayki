rootProject.name = "GoWayki"

include(":androidApp")
include(":shared")
include(":baselineprofile")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }

    plugins {
        val kotlinVersion = extra["kotlin.version"] as String
        val agpVersion = extra["agp.version"] as String
        val composeVersion = extra["compose.version"] as String

        kotlin("jvm").version(kotlinVersion)
        kotlin("multiplatform").version(kotlinVersion)
        kotlin("android").version(kotlinVersion)
        kotlin("plugin.compose").version(kotlinVersion)

        id("com.android.application").version(agpVersion)
        id("com.android.library").version(agpVersion)
        id("com.android.test").version(agpVersion)

        id("org.jetbrains.compose").version(composeVersion)
        id("androidx.baselineprofile").version("1.5.0")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
