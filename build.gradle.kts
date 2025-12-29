// Root build.gradle.kts
// Plugins are managed via buildSrc and version catalogs.
// This file primarily handles global tasks like 'clean'.

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
