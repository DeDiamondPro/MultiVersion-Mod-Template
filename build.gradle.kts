plugins {
    java
    alias(libs.plugins.architecturyPlugin)
    alias(libs.plugins.blossom)
    alias(libs.plugins.shadow)
    id(libs.plugins.loom.get().pluginId)
    id(libs.plugins.preprocessor.get().pluginId)
}

val modPlatform = Platform.of(project)
buildscript {
    // Set loom to the correct platform
    project.extra.set("loom.platform", project.name.substringAfter("-"))
}

architectury {
    platformSetupLoomIde()
    when (modPlatform.loader) {
        Platform.Loader.Fabric -> fabric()
        Platform.Loader.Forge -> forge()
        Platform.Loader.NeoForge -> neoForge()
    }
}

val mod_name: String by project
val mod_version: String by project
val mod_id: String by project
val mod_description: String by project
val mod_license: String by project

version = mod_version
group = "com.example"

blossom {
    replaceToken("@NAME@", mod_name)
    replaceToken("@ID@", mod_id)
    replaceToken("@VERSION@", mod_version)
}

preprocess {
    vars.put("MC", modPlatform.mcVersion)
    vars.put("FABRIC", if (modPlatform.isFabric) 1 else 0)
    vars.put("FORGE", if (modPlatform.isForge) 1 else 0)
    vars.put("NEOFORGE", if (modPlatform.isNeoForge) 1 else 0)
    vars.put("FORGELIKE", if (modPlatform.isForgeLike) 1 else 0)
}

loom {
    if (modPlatform.isForge) forge {
        mixinConfig("$mod_id.mixins.json")
    }
}

repositories {
    maven("https://maven.neoforged.net/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:${modPlatform.mcVersionStr}")
    val yarnVersion = when (modPlatform.mcVersion) {
        12004 -> "1.20.4+build.3"
        else -> error("No mappings defined for ${modPlatform.mcVersion}")
    }
    mappings("net.fabricmc:yarn:${yarnVersion}:v2")

    if (modPlatform.isFabric) {
        modImplementation("net.fabricmc:fabric-loader:0.15.6")
    } else if (modPlatform.isForge) {
        val forgeVersion = when (modPlatform.mcVersion) {
            12004 -> "49.0.26"
            else -> error("No forge version defined for ${modPlatform.mcVersion}")
        }
        "forge"("net.minecraftforge:forge:${modPlatform.mcVersionStr}-$forgeVersion")
    } else if (modPlatform.isNeoForge) {
        val neoforgeVersion = when (modPlatform.mcVersion) {
            12004 -> "20.4.148-beta"
            else -> error("No neoforge version defined for ${modPlatform.mcVersion}")
        }
        "neoForge"("net.neoforged:neoforge:$neoforgeVersion")
    }
}

// Function to get the range of mc versions supported by a version we are building for.
// First value is start of range, second value is end of range or null to leave the range open
val supportedVersionRange: Pair<String, String?> = when (modPlatform.mcVersion) {
    12004 -> "1.20.4" to null
    else -> error("Undefined version range for ${modPlatform.mcVersion}")
}

val prettyVersionRange: String =
    if (supportedVersionRange.first == supportedVersionRange.second) supportedVersionRange.first
    else "${supportedVersionRange.first}${supportedVersionRange.second?.let { "-$it" } ?: "+"}"

val fabricMcVersionRange: String =
    ">=${supportedVersionRange.first}${supportedVersionRange.second?.let { " <=$it" } ?: ""}"

val forgeMcVersionRange: String =
    "[${supportedVersionRange.first},${supportedVersionRange.second?.let { "$it]" } ?: ")"}"

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

base.archivesName = "$mod_name ($prettyVersionRange-${modPlatform.loaderStr})"

tasks {
    processResources {
        val properties = mapOf(
            "id" to mod_id,
            "name" to mod_name,
            "version" to mod_version,
            "description" to mod_description,
            "mcVersionFabric" to fabricMcVersionRange,
            "mcVersionForge" to forgeMcVersionRange,
            "license" to mod_license,
        )
        inputs.properties(properties)
        filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml")) {
            expand(properties)
        }
        if (modPlatform.isFabric) {
            exclude("META-INF/mods.toml", "pack.mcmeta")
        } else {
            exclude("fabric.mod.json")
        }
    }
    shadowJar {
        archiveClassifier.set("dev")
        configurations = listOf(shade)
    }
    remapJar {
        input.set(shadowJar.get().archiveFile)
        archiveClassifier.set("")
        finalizedBy("copyJar")
    }
    register<Copy>("copyJar") {
        File("${project.rootDir}/jars").mkdir()
        from(remapJar.get().archiveFile)
        into("${project.rootDir}/jars")
    }
    clean { delete("${project.rootDir}/jars") }
}

if (modPlatform.isForge) {
    sourceSets.forEach {
        val dir = layout.buildDirectory.dir("sourcesSets/$it.name")
        it.output.setResourcesDir(dir)
        it.java.destinationDirectory = dir
    }
}

data class Platform(
    val mcMajor: Int,
    val mcMinor: Int,
    val mcPatch: Int,
    val loader: Loader
) {
    val mcVersion = mcMajor * 10000 + mcMinor * 100 + mcPatch
    val mcVersionStr = listOf(mcMajor, mcMinor, mcPatch).dropLastWhile { it == 0 }.joinToString(".")
    val loaderStr = loader.toString().lowercase()

    val isFabric = loader == Loader.Fabric
    val isForge = loader == Loader.Forge
    val isNeoForge = loader == Loader.NeoForge
    val isForgeLike = loader == Loader.Forge || loader == Loader.NeoForge
    val isLegacy = mcVersion <= 11202

    override fun toString(): String {
        return "$mcVersionStr-$loaderStr"
    }

    enum class Loader {
        Fabric,
        Forge,
        NeoForge
    }

    companion object {
        fun of(project: Project): Platform {
            val (versionStr, loaderStr) = project.name.split("-", limit = 2)
            val (major, minor, patch) = versionStr.split('.').map { it.toInt() } + listOf(0)
            val loader = Loader.values().first { it.name.lowercase() == loaderStr.lowercase() }
            return Platform(major, minor, patch, loader)
        }
    }
}