plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
