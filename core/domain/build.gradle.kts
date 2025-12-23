plugins {
    id("com.locus.kotlin-jvm")
    alias(libs.plugins.kover)
}

dependencies {
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.archunit.junit5)
}

koverReport {
    verify {
        rule {
            minBound(90)
        }
    }
}
