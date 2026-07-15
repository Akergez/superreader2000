import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    jvmToolchain(21)

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.common)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.desktop.currentOs) // ОБЯЗАТЕЛЬНО

                // Koin for dependency injection
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.kotlinx.serialization)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.akergez.superreader2000.desktop.MainKt"

        nativeDistributions {
            // Target formats: RPM for Linux (x86_64 and arm64), DMG for macOS (arm64)
            targetFormats(TargetFormat.Dmg, TargetFormat.Rpm)

            packageName = "SuperReader2000"
            packageVersion = "1.0.0"
            description = "Cross-platform PDF, DjVu, and FB2 Reader"
            vendor = "Akergez"

            // Include all target architectures
            includeAllModules = true

            buildTypes {
                release {
                    proguard {
                        isEnabled = false
                    }
                }
            }

            linux {
                packageName = "superreader2000"
                debMaintainer = "ragusseven@gmail.com"
                menuGroup = "Office"
                appRelease = "1"
                appCategory = "Office"
                rpmLicenseType = "GPL-3.0"
                // RPM will be built for x86_64 and arm64 based on the build machine
                // To build for specific arch, use: ./gradlew packageRpm -PtargetArch=<arch>
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }

            macOS {
                bundleID = "com.akergez.superreader2000"
                dockName = "SuperReader2000"
                appStore = false
                // Build for arm64 (Apple Silicon)
                // DMG will be created for the target architecture
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }
        }
    }
}
