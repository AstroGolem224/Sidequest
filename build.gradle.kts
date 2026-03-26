plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

val localBuildRoot = System.getenv("LOCALAPPDATA")
    ?.let(::file)
    ?.resolve("SidequestBuild")
    ?: rootDir.resolve(".local-build")

rootProject.layout.buildDirectory.set(localBuildRoot.resolve("root"))

subprojects {
    val projectBuildPath = project.path
        .trimStart(':')
        .replace(':', '/')
        .ifBlank { "root" }
    layout.buildDirectory.set(localBuildRoot.resolve(projectBuildPath))
}
