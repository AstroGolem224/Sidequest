pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Sidequest"

include(
    ":app",
    ":core-ui",
    ":core-data",
    ":feature-capture",
    ":feature-inbox",
    ":feature-missions",
    ":feature-lobby",
    ":feature-search",
    ":feature-settings",
)
