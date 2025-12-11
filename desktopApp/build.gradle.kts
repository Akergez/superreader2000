import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    jvmToolchain(17)

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
        mainClass = "com.example.bottomapp.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PDFReaderKMP"
            packageVersion = "1.0.0"
            description = "Cross-platform PDF, DjVu, and FB2 Reader"
            vendor = "PDFReaderKMP"

            windows {
                shortcut = true
                upgradeUuid = "D6E29113-99C0-42B0-9929-F0151DA11000"
            }

            linux {
                packageName = "pdfreaderkmp"
                debMaintainer = "support@pdfreaderkmp.com"
                menuGroup = "Office"
                appRelease = "1"
                appCategory = "Office"
                rpmLicenseType = "Apache-2.0"
            }

            macOS {
                bundleID = "com.example.pdfreaderkmp"
                dockName = "PDF Reader KMP"
                appStore = false
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

