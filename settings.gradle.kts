pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "SuperReader2000"
include(":superreader2000")
include(":shared")
include(":desktopApp")
val tresseKmpDir = file(
    System.getenv("TRESSE_KMP_DIR")
        ?: "../tresse-kmp"
)
check(tresseKmpDir.resolve("build.gradle.kts").isFile) {
    "tresse-kmp not found at $tresseKmpDir; set TRESSE_KMP_DIR"
}
include(":tresse-kmp")
project(":tresse-kmp").projectDir = tresseKmpDir
