import org.gradle.kotlin.dsl.implementation

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    publicResClass = true
    generateResClass = always
}
kotlin {
    jvm("desktop")
    androidTarget {
    }

    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {

                implementation(libs.kotlinx.serialization)
                implementation(libs.zoomable)
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

                // SQLDelight for database
                implementation(libs.sqldelight.runtime)
                implementation(compose.components.resources)
                //implementation(libs.oleksandrbalan.pagecurl)
                implementation(libs.material.icons.extended) // Check for the latest version

                // Navigation Compose for KMP
                implementation(libs.jetbrains.navigation.compose)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
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
                // Apache PDFBox for PDF processing (Desktop/JVM only)
                implementation(libs.pdfbox)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
            }
        }

        val androidMain by getting {
            dependencies {
                // Android Framework - needed for IDE to resolve android.* imports
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.activity:activity:1.8.0")

                implementation(libs.androidx.activity.compose.v180)
                implementation(libs.androidx.appcompat.v161)
                implementation(libs.androidx.core)
                implementation(libs.androidx.documentfile)
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

                // Android Room & navigation
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.room.common)
                implementation(libs.androidx.constraintlayout)
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.androidx.room.ktx)
                // optional - RxJava2 support for Room
                implementation(libs.androidx.room.rxjava2)
                // optional - RxJava3 support for Room
                implementation(libs.androidx.room.rxjava3)
                // optional - Guava support for Room, including Optional and ListenableFuture
                implementation(libs.androidx.room.guava)
                // optional - Test helpers
                // optional - Paging 3 Integration
                implementation(libs.androidx.room.paging)

                // PDF & DjVu rendering (Android specific)
                implementation(libs.pdfbox.android)
                implementation(libs.djvulibre)

                // Image loading
                implementation(libs.coil.compose.v210)

                // Navigation & UI
                implementation(libs.accompanist.pager)
                implementation(libs.oleksandrbalan.pagecurl)
                implementation(libs.zoomable)

                // Coil image loader
                implementation(libs.coil.compose.v210)


                implementation(libs.androidx.activity.ktx)
                implementation(libs.androidx.core.ktx.v1120)
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
}

// Add KSP Room compiler for Android and Desktop
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

