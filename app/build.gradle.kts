import kotlinx.kover.gradle.plugin.dsl.MetricType
import java.io.ByteArrayOutputStream

plugins {
    id("com.locus.android-app")
    alias(libs.plugins.kover)
}

fun getGitVersionName(): String {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "describe", "--tags")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        val output = stdout.toString().trim()
        if (output.isNotEmpty()) output else "0.0.0-dev"
    } catch (e: Exception) {
        "0.0.0-dev"
    }
}

fun getGitVersionCode(): Int {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        val output = stdout.toString().trim()
        if (output.isNotEmpty()) output.toInt() else 1
    } catch (e: Exception) {
        1
    }
}

android {
    namespace = "com.locus.android"

    defaultConfig {
        applicationId = "com.locus.android"
        versionCode = getGitVersionCode()
        versionName = getGitVersionName()
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    androidTestImplementation(project(":core:testing"))

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Security
    implementation(libs.tink.android)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Testing
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}

koverReport {
    verify {
        rule {
            bound {
                minValue = 70
                metric = MetricType.INSTRUCTION
            }
            bound {
                minValue = 70
                metric = MetricType.BRANCH
            }
        }
    }
}
