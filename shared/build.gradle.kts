plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.library)
}

kotlin {
    jvm("desktop")
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                // Koin for dependency injection
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                // Serialization
                implementation(libs.kotlinx.serialization)

                // PDFBox for PDF processing
                implementation(libs.pdfbox)

                // SQLDelight for database
                implementation(libs.sqldelight.runtime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.sqlite)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.appcompat)
                implementation(libs.core.ktx)
                implementation(libs.activity.compose)
                implementation(libs.lifecycle.runtime.ktx)
                implementation(libs.androidx.lifecycle.viewmodel.ktx)
                implementation(libs.androidx.lifecycle.livedata.ktx)
                implementation(libs.material)
                implementation(libs.material3)
                implementation(libs.androidx.material3)

                // Android-specific Koin
                implementation(libs.koin.androidx.compose.v400)
                implementation(libs.koin.androidx.compose.navigation)

                // Android Room & navigation
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.navigation.fragment.ktx)
                implementation(libs.androidx.navigation.ui.ktx)
                implementation(libs.androidx.constraintlayout)
                implementation(libs.androidx.appcompat)

                // PDF & DjVu rendering (Android specific)
                implementation(libs.pdfbox.android)
                implementation(libs.djvulibre)

                // Image loading
                implementation(libs.coil.compose.v210)

                // Navigation & UI
                implementation(libs.navigation.compose)
                implementation(libs.accompanist.pager)
                implementation(libs.accompanist.navigation.animation)
                implementation(libs.oleksandrbalan.pagecurl)
                implementation(libs.zoomable)

                // Coil image loader
                implementation(libs.coil.compose.v210)

                // GSON for serialization
                implementation(libs.gson)
            }
        }
    }
}

android {
    namespace = "com.example.bottomapp.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

