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
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    // Compose Compiler Plugin
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.0.0")
    // Hilt Plugin
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.51")
    // KSP Plugin
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.0-1.0.21")
    // KTLint Plugin
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.1.0")
    // Gradle Play Publisher
    implementation("com.github.triplet.gradle:play-publisher:3.9.1")
}
