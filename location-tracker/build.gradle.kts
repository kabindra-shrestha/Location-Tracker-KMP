import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.kabindra.locationtracker"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    val xcf = XCFramework("LocationTracker")
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "LocationTracker"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.compose.runtime)
                api(libs.compass.geolocation)
                implementation(libs.compass.geolocation.mobile)
                implementation(libs.compass.permissions.mobile)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.play.services.location)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.service)
                implementation(libs.androidx.activity.compose)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.core)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.testExt.junit)
            }
        }

        iosMain {
            dependencies {
                // CLLocationManager is accessed directly via Kotlin/Native cinterop
                // (platform.CoreLocation), no extra dependency needed here.
            }
        }
    }

}

// ---------------------------------------------------------------------------
// Publishing to GitHub Packages
//
// Usage from a consuming project's settings.gradle.kts:
//
//   dependencyResolutionManagement {
//       repositories {
//           maven {
//               url = uri("https://maven.pkg.github.com/<owner>/<repo>")
//               credentials {
//                   username = providers.gradleProperty("gpr.user").orNull
//                   password = providers.gradleProperty("gpr.token").orNull
//               }
//           }
//       }
//   }
//
// Then: implementation("com.kabindra:location-tracker:1.0.0")
// ---------------------------------------------------------------------------
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri(
                "https://maven.pkg.github.com/${project.findProperty("GITHUB_OWNER")}/${
                    project.findProperty(
                        "GITHUB_REPO"
                    )
                }"
            )
            credentials {
                username =
                    System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                password =
                    System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.token") as String?
            }
        }
        maven {
            name = "GitLab"
            url = uri("${project.findProperty("GITLAB_API_URL")}/projects/${project.findProperty("GITLAB_PROJECT_ID")}/packages/maven")
            credentials {
                username = System.getenv("GITLAB_USER") ?: "Job-Token"
                password = System.getenv("GITLAB_TOKEN") ?: System.getenv("CI_JOB_TOKEN")
            }
        }
    }

    publications.withType<MavenPublication>().configureEach {
        groupId = project.findProperty("GROUP") as String
        version = project.findProperty("VERSION_NAME") as String

        pom {
            name.set("KMP Location Tracker")
            description.set(
                "Kotlin Multiplatform location tracking with an Android foreground " +
                        "service and iOS background CLLocationManager, built on top of Compass."
            )
            url.set(
                "https://github.com/${project.findProperty("GITHUB_OWNER")}/${
                    project.findProperty(
                        "GITHUB_REPO"
                    )
                }"
            )
            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
        }
    }
}
