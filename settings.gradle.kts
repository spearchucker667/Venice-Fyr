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

rootProject.name = "VeniceForgeAndroid"
include(":app")
include(":venice-sdk")
include(":core:common")
include(":core:security")
include(":core:designsystem")
include(":core:data")
