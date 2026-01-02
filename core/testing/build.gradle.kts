plugins {
    id("com.locus.android-library")
}

android {
    namespace = "com.locus.core.testing"
}

dependencies {
    implementation(project(":core:domain"))

    // Testing - exported as API so consuming modules get them
    api(libs.junit)
    api(libs.androidx.junit)
    api(libs.androidx.espresso.core)
    api(libs.mockk)
    api(libs.mockk.android)
    api(libs.turbine)
    api(libs.truth)
    api(libs.robolectric)

    // Coroutines for testing
    api(libs.kotlinx.coroutines.test)
}
