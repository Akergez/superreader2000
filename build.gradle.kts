// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    dependencies {
        // Add this line
        classpath(libs.kotlin.gradle.plugin)
    }
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // Add this line
    }
}

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.library) apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
    alias(libs.plugins.kotlinCompose) apply false

}
true // Needed to make the Suppress annotation work for the plugins block

