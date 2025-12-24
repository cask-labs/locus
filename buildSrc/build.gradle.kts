plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Android Gradle Plugin
    implementation("com.android.tools.build:gradle:8.2.0")
    // Kotlin Plugin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    // Hilt Plugin
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.51")
    // KSP Plugin
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.22-1.0.17")
    // KTLint Plugin
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.0")
    // CycloneDX Plugin
    implementation("org.cyclonedx:cyclonedx-gradle-plugin:1.8.1")
    // Gradle Play Publisher
    implementation("com.github.triplet.gradle:play-publisher:3.9.1")
}
