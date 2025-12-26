import kotlinx.kover.gradle.plugin.dsl.MetricType

plugins {
    id("com.locus.android-library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kover)
}

android {
    namespace = "com.locus.core.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // AWS
    implementation(libs.aws.sdk.s3)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}

koverReport {
    verify {
        rule {
            // Instruction Coverage (Existing)
            bound {
                minValue = 80
                metric = MetricType.INSTRUCTION
            }
            // Branch Coverage (New)
            bound {
                minValue = 80
                metric = MetricType.BRANCH
            }
        }
    }
}
