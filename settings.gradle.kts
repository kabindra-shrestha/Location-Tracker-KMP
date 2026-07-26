@file:Suppress("UnstableApiUsage")

rootProject.name = "LocationTrackerKMP"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
    }
}

// The publishable library module — this is the only module you need
// if you are consuming this repo as a git submodule / composite build
// instead of via GitHub Packages.
include(":location-tracker")

// A minimal Compose Multiplatform demo, split per the recommended KMP
// structure: app entry points live in their own modules, separate from
// shared code (https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html).
// Safe to remove the whole `sample` folder if you only want the library module.
include(":shared")
include(":androidApp")
