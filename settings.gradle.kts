pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "AniFlow"

// Composite Build for Build Logic Conventions
includeBuild("build-logic")

// App (Composition Root)
include(":app")

// Core
include(":core:common")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:storage")
include(":core:logging")

// Domain (Pure Business Model)
include(":domain")
include(":domain:release-intelligence")
include(":domain:selection")

// Data (Repository Implementations)
include(":data")

// Providers
include(":provider:core")
include(":provider:nyaa")

// Download Engines & Service
include(":download:core")
include(":download:http")
include(":download:torrent")
include(":download:service")

// Platform Subsystems
include(":platform:notifications")
include(":platform:share")
include(":platform:network")
include(":platform:storage")

// Feature Modules
include(":feature:home")
include(":feature:search")
include(":feature:release")
include(":feature:collections")
include(":feature:downloads")
include(":feature:library")
include(":feature:storage")
include(":feature:favorites")
include(":feature:settings")
include(":feature:automation")
include(":feature:watchlist")
include(":feature:saved-search")

// Testing
include(":testing")
