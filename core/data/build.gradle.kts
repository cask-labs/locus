import kotlinx.kover.gradle.plugin.dsl.MetricType

plugins {
    id("com.locus.android-library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinSerialization)
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
    implementation(libs.aws.sdk.cloudformation)
    implementation(libs.aws.sdk.sts)

    // DataStore & Security
    implementation(libs.androidx.datastore)
    implementation(libs.tink.android)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
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
