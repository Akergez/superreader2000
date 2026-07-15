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
        if (providers.environmentVariable("TRESSE_KMP_USE_MAVEN_LOCAL").orNull == "true") {
            mavenLocal()
        }
        maven {
            name = "TresseGitLab"
            url = uri(providers.environmentVariable("TRESSE_KMP_MAVEN_URL")
                .orElse("https://gitlab.com/api/v4/projects/84164267/packages/maven").get())
            content { includeGroup("com.akergez.tresse") }
        }
    }
}

rootProject.name = "SuperReader2000"
include(":superreader2000")
include(":shared")
include(":desktopApp")
