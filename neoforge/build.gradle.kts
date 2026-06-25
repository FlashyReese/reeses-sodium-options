import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("idea")
    id("java-library")
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val MINECRAFT_VERSION = rootProject.extra["MINECRAFT_VERSION"] as String
val NEOFORGE_VERSION = rootProject.extra["NEOFORGE_VERSION"] as String
val SODIUM_VERSION = rootProject.extra["SODIUM_VERSION"] as String
val CONTROLIFY_VERSION = rootProject.extra["CONTROLIFY_VERSION"] as String
val CONTROLIFY_ENABLED = rootProject.extra["CONTROLIFY_ENABLED"] as Boolean
val SODIUM_NEOFORGE_RUNTIME_MODS = listOf(
    "org.sinytra.forgified-fabric-api:fabric-api-base:0.4.42+d1308ded19",
    "org.sinytra.forgified-fabric-api:fabric-renderer-api-v1:3.4.1+9125b6dc19",
    "org.sinytra.forgified-fabric-api:fabric-rendering-data-attachment-v1:0.3.48+73761d2e19",
    "org.sinytra.forgified-fabric-api:fabric-block-view-api-v2:1.0.10+9afaaf8c19",
)

base {
    archivesName.set("${rootProject.name}-neoforge")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

repositories {
    maven("https://maven.neoforged.net/releases/")
}

loom {
    runs {
        named("client") {
            client()
            displayName.set("NeoForge Client")
            runDirectory.set(layout.projectDirectory.dir("run"))
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

configurations.named("developmentNeoForge") {
    extendsFrom(common)
}

dependencies {
    minecraft("com.mojang:minecraft:$MINECRAFT_VERSION")
    mappings(loom.layered {
        officialMojangMappings()
    })
    add("neoForge", "net.neoforged:neoforge:$NEOFORGE_VERSION")

    // Sodium's NeoForge wrapper provides runtime services; the nested mod jar is needed as a real mod in Loom dev runs.
    modImplementation("net.caffeinemc:sodium-neoforge-mod:$SODIUM_VERSION")
    add("forgeRuntimeLibrary", "net.caffeinemc:sodium-neoforge:$SODIUM_VERSION") {
        isTransitive = false
    }
    if (CONTROLIFY_ENABLED) {
        modCompileOnly("dev.isxander:controlify:$CONTROLIFY_VERSION-neoforge") {
            isTransitive = false
        }
    }
    SODIUM_NEOFORGE_RUNTIME_MODS.forEach {
        modRuntimeOnly(it)
    }
    add("common", project(path = ":common", configuration = "namedElements")) {
        isTransitive = false
    }
    add("shadowBundle", project(path = ":common", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }
}

tasks.named("compileTestJava").configure {
    enabled = false
}

tasks.test {
    failOnNoDiscoveredTests = false
}

tasks.withType<JavaExec>().configureEach {
    if (name.contains("dev.architectury.transformer.TransformerRuntime.main")) {
        dependsOn(
            "generateDLIConfig",
            "prepareArchitecturyTransformer",
            "configureClientLaunch",
        )
        classpath += configurations.named("runtimeClasspath").get()
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        inputs.property("controlifyEnabled", CONTROLIFY_ENABLED)

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mapOf("version" to project.version))
        }

        if (!CONTROLIFY_ENABLED) {
            doLast {
                val metadata = layout.buildDirectory.file("resources/main/META-INF/neoforge.mods.toml").get().asFile
                val text = metadata.readText()
                    .replace(Regex("""\[\[dependencies\.reeses_sodium_options]]\RmodId = "controlify"\Rtype = "optional"\RversionRange = "\[[^"]+"\Rordering = "AFTER"\Rside = "CLIENT"\R\R?"""), "")

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
    archiveClassifier.set("dev-shadow")
    from(rootDir.resolve("LICENSE.txt"))
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    dependsOn(tasks.named("shadowJar"))
    inputFile.set(tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
    archiveClassifier.set("")
}

configurations.named("runtimeElements") {
    outgoing.artifacts.clear()
}

artifacts {
    add("runtimeElements", tasks.named("remapJar"))
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
