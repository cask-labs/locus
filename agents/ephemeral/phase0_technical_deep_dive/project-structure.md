# Deep Dive: Project Structure & Gradle Configuration

## Context

Establishing a robust multi-module Gradle setup is critical for the "Tracer Bullet" phase. This document defines the exact configuration for the Version Catalog (`libs.versions.toml`) and the build scripts for the core modules, ensuring strict dependency management and layer isolation.

## 1. Version Catalog (`gradle/libs.versions.toml`)

This file acts as the single source of truth for all dependencies.

```toml
[versions]
# Plugins
agp = "8.2.0"
kotlin = "1.9.22"
hilt = "2.50"
ksp = "1.9.22-1.0.17" # Matches Kotlin 1.9.22

# Android
coreKtx = "1.12.0"
lifecycle = "2.7.0"
activityCompose = "1.8.2"
composeBom = "2024.02.00"

# Third Party
coroutines = "1.7.3"
javaxInject = "1"

# Testing
junit = "4.13.2"
junitExt = "1.1.5"
espresso = "3.5.1"
mockk = "1.13.9"
truth = "1.4.0"
turbine = "1.0.0"

[libraries]
# Android Core
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }

# Compose (BOM)
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

# DI
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
javax-inject = { group = "javax.inject", name = "javax.inject", version.ref = "javaxInject" }

# Concurrency
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitExt" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
mockk-android = { group = "io.mockk", name = "mockk-android", version.ref = "mockk" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

[bundles]
compose = ["androidx-compose-ui", "androidx-compose-ui-graphics", "androidx-compose-ui-tooling-preview", "androidx-compose-material3"]
```

## 2. Module Configuration Templates

### Root `build.gradle.kts`

Handles common configuration for all modules.

```kotlin
// Root build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

### `:app` (Android Application)

The UI entry point and DI root.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.locus.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.locus.android"
        minSdk = 28
        targetSdk = 34
        versionCode = 1 // Logic to fetch from git to be added
        versionName = "0.0.1" // Logic to fetch from git to be added

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Build Types & Flavors defined here (standard/foss)
    // ...
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

### `:core:domain` (Pure Kotlin)

Strictly no Android dependencies (except maybe minimal annotations if needed, but ideally pure).

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.javax.inject) // JSR-330 Standard
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
```

### `:core:data` (Android Library)

Implements domain repositories; depends on Android APIs (Room, Location, etc.).

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.locus.core.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(projects.core.domain)

    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
```

## Decisions

1.  **Version Catalog:** We strictly use TOML for dependency management.
2.  **Plugin Aliases:** We use `alias(libs.plugins...)` for type-safe plugin application.
3.  **Strict Isolation:** `:core:domain` uses `kotlin-jvm` plugin, preventing accidental access to Android APIs.
4.  **KSP:** We use KSP (Kotlin Symbol Processing) instead of KAPT for Hilt/Room from the start for performance.
