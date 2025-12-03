// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.library) apply false    // CHANGE THIS LINE from "2.0.0" to "2.0.21"
    id("org.jetbrains.kotlin.multiplatform") version "2.2.21" apply false
    id("org.jetbrains.compose") version "1.9.3" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
    alias(libs.plugins.kotlinCompose) apply false
    id("app.cash.sqldelight") version "2.2.1" apply false
}


