plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
    id("kotlin-parcelize")
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.example.bottomapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bottomapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }
}

dependencies {
    implementation(libs.djvulibre)
    implementation(libs.zoomable)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material)
    implementation(libs.pdfbox.android)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose.v240alpha10)
    implementation(libs.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.room.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.navigation.compose)
    implementation(libs.oleksandrbalan.pagecurl)
    implementation(libs.accompanist.pager)
    implementation(libs.gson)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.coil.compose.v210)
    implementation(libs.koin.androidx.compose.v400)
    implementation(libs.koin.androidx.compose.navigation)
    implementation(libs.androidx.room.runtime)
    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    //annotationProcessor("androidx.room:room-compiler:$room_version")
    ksp(libs.androidx.room.compiler)
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation(libs.androidx.room.ktx)
    // optional - RxJava2 support for Room
    implementation(libs.androidx.room.rxjava2)
    // optional - RxJava3 support for Room
    implementation(libs.androidx.room.rxjava3)
    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation(libs.androidx.room.guava)
    // optional - Test helpers
    testImplementation(libs.androidx.room.testing)
    // optional - Paging 3 Integration
    implementation(libs.androidx.room.paging)
}