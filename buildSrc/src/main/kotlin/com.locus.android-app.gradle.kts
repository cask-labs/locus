import java.util.Properties
import java.io.File
import java.util.Base64

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.cyclonedx.bom")
    id("com.github.triplet.play")
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 28
        targetSdk = 34
        vectorDrawables {
            useSupportLibrary = true
        }
        // Fix for missing dimension strategy when dependencies (like :core:testing)
        // do not have the 'distribution' flavor dimension.
        missingDimensionStrategy("distribution", "standard")
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {
            dimension = "distribution"
        }
        create("foss") {
            dimension = "distribution"
            // Allow foss builds to consume standard libraries if needed
            matchingFallbacks.add("standard")
        }
    }

    signingConfigs {
        create("release") {
            val keystoreBase64 = System.getenv("LOCUS_UPLOAD_KEYSTORE_BASE64")
            if (keystoreBase64 != null) {
                // CI Environment: Decode Keystore
                val keystoreFile = File(System.getenv("RUNNER_TEMP") ?: "/tmp", "upload.jks")
                val bytes = Base64.getDecoder().decode(keystoreBase64)
                keystoreFile.writeBytes(bytes)
                storeFile = keystoreFile
                storePassword = System.getenv("LOCUS_STORE_PASSWORD")
                keyAlias = System.getenv("LOCUS_KEY_ALIAS")
                keyPassword = System.getenv("LOCUS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // TODO: Temporary workaround for CI - Only sign if the keystore is present.
            // Long term we want to enforce signed APKs for all release builds.
            if (System.getenv("LOCUS_UPLOAD_KEYSTORE_BASE64") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

play {
    val jsonContent = System.getenv("LOCUS_PLAY_JSON")
    if (jsonContent != null) {
        val jsonFile = File(System.getenv("RUNNER_TEMP") ?: "/tmp", "play-settings.json")
        jsonFile.writeText(jsonContent)
        serviceAccountCredentials.set(jsonFile)
    }
    track.set("internal")
    defaultToAppBundles.set(true)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
