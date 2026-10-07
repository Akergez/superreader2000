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
            name = "TresseGitHub"
            url = uri(providers.environmentVariable("TRESSE_KMP_MAVEN_URL")
                .orElse("https://maven.pkg.github.com/akergez/tresse-kmp").get())
            // GitHub Packages asks for a token (read:packages) even for public packages.
            val githubToken = providers.gradleProperty("gpr.key")
                .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            if (githubToken != null) {
                credentials {
                    username = providers.gradleProperty("gpr.user")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR")).getOrElse("token")
                    password = githubToken
                }
            }
            content { includeGroup("com.akergez.tresse") }
        }
    }
}

rootProject.name = "SuperReader2000"
include(":superreader2000")
include(":shared")
include(":desktopApp")
