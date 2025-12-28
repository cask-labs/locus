import kotlinx.kover.gradle.plugin.dsl.MetricType

plugins {
    id("com.locus.kotlin-jvm")
    alias(libs.plugins.kover)
    alias(libs.plugins.pitest)
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
            // Instruction Coverage (Existing)
            bound {
                minValue = 79
                metric = MetricType.INSTRUCTION
            }
            // Branch Coverage (New)
            bound {
                minValue = 79
                metric = MetricType.BRANCH
            }
        }
    }
}

configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
    // JUnit 5 support
    junit5PluginVersion.set(libs.versions.pitest.junit5.get())

    // Target classes (Domain Layer)
    targetClasses.set(setOf("com.locus.core.domain.**"))

    // Performance: Auto-detect threads
    threads.set(Runtime.getRuntime().availableProcessors())

    // Output formats
    outputFormats.set(setOf("XML", "HTML"))

    // Incremental Analysis Configuration
    // The history file is stored in the build directory during execution
    historyInputLocation.set(layout.buildDirectory.file("pitest/history.bin"))
    historyOutputLocation.set(layout.buildDirectory.file("pitest/history.bin"))

    // Avoid cluttering with timestamps
    timestampedReports.set(false)
}

// Custom Task: Initialize Pitest History
// Checks if local history exists (build dir). If not, tries to copy from committed baseline.
tasks.register("initializePitestHistory") {
    group = "verification"
    description = "Initializes PITest history from committed baseline if local history is missing."

    doLast {
        val buildHistory = layout.buildDirectory.file("pitest/history.bin").get().asFile
        val committedHistory = project.file("pitest-history.bin")

        if (!buildHistory.exists() && committedHistory.exists()) {
            println("Copying committed PITest history to build directory...")
            buildHistory.parentFile.mkdirs()
            committedHistory.copyTo(buildHistory, overwrite = true)
        } else if (buildHistory.exists()) {
            println("Local PITest history found. Using existing incremental data.")
        } else {
            println("No PITest history found. Starting fresh.")
        }
    }
}

// Make 'pitest' task depend on initialization
tasks.named("pitest") {
    dependsOn("initializePitestHistory")
}

// Custom Task: Update Pitest Baseline
// Copies the current local history (build dir) back to the source tree for committing.
tasks.register("updatePitestBaseline") {
    group = "verification"
    description = "Promotes the current local PITest history to the committed baseline."

    doLast {
        val buildHistory = layout.buildDirectory.file("pitest/history.bin").get().asFile
        val committedHistory = project.file("pitest-history.bin")

        if (buildHistory.exists()) {
            println("Updating committed PITest baseline from local history...")
            buildHistory.copyTo(committedHistory, overwrite = true)
            println("Updated: ${committedHistory.absolutePath}")
            println("You can now commit this file to Git.")
        } else {
            throw GradleException(
                "No local PITest history found at ${buildHistory.absolutePath}. " +
                    "Run './gradlew :core:domain:pitest' first.",
            )
        }
    }
}
