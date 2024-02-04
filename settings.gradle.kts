pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net")
        maven("https://jitpack.io") {
            content {
                includeGroupByRegex("com\\.github\\..*")
            }
        }
    }

    resolutionStrategy.eachPlugin {
        when (requested.id.id) {
            "com.replaymod.preprocess" -> useModule("com.github.replaymod:preprocessor:${requested.version}")
            "com.replaymod.preprocess-root" -> useModule("com.github.replaymod:preprocessor:${requested.version}")
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs.create("libs")
}

rootProject.buildFileName = "root.gradle.kts"

listOf(
    "1.20.4-fabric",
    "1.20.4-forge",
    "1.20.4-neoforge"
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}