plugins {
    id("java")
    id("dev.architectury.loom") version "1.17.483" apply false
    id("architectury-plugin") version "3.5.169"
    id("com.gradleup.shadow") version "9.3.0" apply false
}

val MINECRAFT_VERSION = "1.21.1"
val NEOFORGE_VERSION = "21.1.234"
val FABRIC_LOADER_VERSION = "0.19.3"
val FABRIC_API_VERSION = "0.116.12+1.21.1"
val CONTROLIFY_VERSION = "3.0.0+lts+1.21.1"
val CONTROLIFY_ENABLED = providers.gradleProperty("rso.enableControlify")
    .map { it.toBoolean() }
    .orElse(true)
    .get()

// https://semver.org/
val MAVEN_GROUP = providers.gradleProperty("maven_group").get()
val ARCHIVE_NAME = providers.gradleProperty("archives_name").get()
val MOD_VERSION = "2.1.0"
val SODIUM_VERSION = "0.8.12-beta.2+mc1.21.1"

extra["MINECRAFT_VERSION"] = MINECRAFT_VERSION
extra["NEOFORGE_VERSION"] = NEOFORGE_VERSION
extra["FABRIC_LOADER_VERSION"] = FABRIC_LOADER_VERSION
extra["FABRIC_API_VERSION"] = FABRIC_API_VERSION
extra["SODIUM_VERSION"] = SODIUM_VERSION
extra["CONTROLIFY_VERSION"] = CONTROLIFY_VERSION
extra["CONTROLIFY_ENABLED"] = CONTROLIFY_ENABLED

architectury {
    minecraft = MINECRAFT_VERSION
}

allprojects {
    group = MAVEN_GROUP
    version = createVersionString()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://api.modrinth.com/maven")
        maven("https://libraries.minecraft.net/")
        maven("https://maven.su5ed.dev/releases")
        maven("https://maven.caffeinemc.net/releases")
        maven("https://maven.caffeinemc.net/snapshots")
        maven("https://maven.isxander.dev/releases")
    }

    base {
        archivesName = "$ARCHIVE_NAME-${project.name}"
    }

    java.toolchain.languageVersion = JavaLanguageVersion.of(21)

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }
}

fun createVersionString(): String {
    val builder = StringBuilder()

    val isReleaseBuild = project.hasProperty("build.release")
    val buildId = System.getenv("GITHUB_RUN_NUMBER")

    if (isReleaseBuild) {
        builder.append(MOD_VERSION)
    } else {
        builder.append(MOD_VERSION.split('-')[0])
        builder.append("-snapshot")
    }

    builder.append("+mc").append(MINECRAFT_VERSION)

    if (!isReleaseBuild) {
        if (buildId != null) {
            builder.append("-build.$buildId")
        } else {
            builder.append("-local")
        }
    }

    return builder.toString()
}
