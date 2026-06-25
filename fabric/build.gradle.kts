import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("idea")
    id("dev.architectury.loom-no-remap")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val MINECRAFT_VERSION = rootProject.extra["MINECRAFT_VERSION"] as String
val FABRIC_LOADER_VERSION = rootProject.extra["FABRIC_LOADER_VERSION"] as String
val FABRIC_API_VERSION = rootProject.extra["FABRIC_API_VERSION"] as String

val SODIUM_VERSION = rootProject.extra["SODIUM_VERSION"] as String
val CONTROLIFY_VERSION = rootProject.extra["CONTROLIFY_VERSION"] as String
val CONTROLIFY_ENABLED = rootProject.extra["CONTROLIFY_ENABLED"] as Boolean

base {
    archivesName.set("${rootProject.name}-fabric")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    mods {
        create("reeses-sodium-options") {
            sourceSet("main")
            sourceSet("main", ":common")
        }
    }

    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

val common = configurations.create("common") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val shadowBundle = configurations.create("shadowBundle") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

configurations.named("compileClasspath") {
    extendsFrom(common)
}

configurations.named("runtimeClasspath") {
    extendsFrom(common)
}

configurations.named("developmentFabric") {
    extendsFrom(common)
}

dependencies {
    minecraft("net.minecraft:minecraft:$MINECRAFT_VERSION")
    implementation("net.fabricmc:fabric-loader:$FABRIC_LOADER_VERSION")

    fun addEmbeddedFabricModule(name: String) {
        val module = fabricApi.module(name, FABRIC_API_VERSION)
        implementation(module)
    }

    // Fabric API modules
    addEmbeddedFabricModule("fabric-api-base")
    addEmbeddedFabricModule("fabric-block-getter-api-v2")
    addEmbeddedFabricModule("fabric-rendering-v1")
    implementation("net.caffeinemc:sodium-fabric:$SODIUM_VERSION")
    if (CONTROLIFY_ENABLED) {
        compileOnly("dev.isxander:controlify:$CONTROLIFY_VERSION-fabric") {
            isTransitive = false
        }
    }
    add("common", project(":common")) {
        isTransitive = false
    }
    add("shadowBundle", project(path = ":common", configuration = "runtimeElements")) {
        isTransitive = false
    }
}

tasks.test {
    failOnNoDiscoveredTests = false
}

tasks.withType<JavaExec>().configureEach {
    if (name.contains("dev.architectury.transformer.TransformerRuntime.main")) {
        dependsOn(
            "generateDLIConfig",
            "prepareArchitecturyTransformer",
            "configureLaunch",
            "configureClientLaunch",
        )
        classpath += configurations.named("runtimeClasspath").get()
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        inputs.property("controlifyEnabled", CONTROLIFY_ENABLED)

        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }

        if (!CONTROLIFY_ENABLED) {
            doLast {
                val metadata = layout.buildDirectory.file("resources/main/fabric.mod.json").get().asFile
                val text = metadata.readText()
                    .replace(Regex("""    "controlify": \[\R      "me\.flashyreese\.mods\.reeses_sodium_options\.client\.controlify\.ReeseSodiumOptionsControlifyEntrypoint"\R    ],\R"""), "")
                    .replace(Regex("""  "suggests": \{\R    "controlify": "[^"]+"\R  },\R"""), "")

                metadata.writeText(text)
            }
        }
    }

    jar {
        archiveClassifier.set("dev")
        from(rootDir.resolve("LICENSE.txt"))
    }
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shadowBundle)
    archiveClassifier.set("")
    from(rootDir.resolve("LICENSE.txt"))
}

configurations.named("runtimeElements") {
    outgoing.artifacts.clear()
}

artifacts {
    add("runtimeElements", tasks.named("shadowJar"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "FlashyReeseReleases"
            url = uri("https://maven.flashyreese.me/releases")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
        maven {
            name = "FlashyReeseSnapshots"
            url = uri("https://maven.flashyreese.me/snapshots")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
