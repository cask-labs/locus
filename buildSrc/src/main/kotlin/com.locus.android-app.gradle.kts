import java.util.Properties
import java.io.File
import java.util.Base64

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    id("com.github.triplet.play")
}

detekt {
    config.setFrom(files("${project.rootDir}/detekt.yml"))
    buildUponDefaultConfig = true
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
            if (!keystoreBase64.isNullOrEmpty()) {
                // CI Environment: Decode Keystore
                try {
                    val keystoreFile = File(System.getenv("RUNNER_TEMP") ?: "/tmp", "upload.jks")
                    // Use MimeDecoder to be more robust against newlines in the env variable
                    val bytes = Base64.getMimeDecoder().decode(keystoreBase64)
                    keystoreFile.writeBytes(bytes)
                    storeFile = keystoreFile
                    storePassword = System.getenv("LOCUS_STORE_PASSWORD")
                    keyAlias = System.getenv("LOCUS_KEY_ALIAS")
                    keyPassword = System.getenv("LOCUS_KEY_PASSWORD")
                } catch (e: Exception) {
                    logger.warn("WARNING: Failed to decode signing keystore. Ensure LOCUS_UPLOAD_KEYSTORE_BASE64 is a valid MIME Base64 string (Base64.getMimeDecoder(), newlines allowed). Error: ${e.message}")
                }
            } else {
                logger.info("INFO: LOCUS_UPLOAD_KEYSTORE_BASE64 is not set. Release APKs will be unsigned.")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Only sign if the signing config was successfully initialized with a store file.
            // This handles cases where LOCUS_UPLOAD_KEYSTORE_BASE64 is empty or invalid.
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null) {
                signingConfig = releaseConfig
            } else {
                logger.warn("WARNING: Signing skipped. Keystore file not found or invalid.")
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
