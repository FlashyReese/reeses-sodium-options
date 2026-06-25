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

configurations.named("developmentNeoForge") {
    extendsFrom(common)
}

dependencies {
    minecraft("com.mojang:minecraft:$MINECRAFT_VERSION")
    mappings(loom.layered {
        officialMojangMappings()
    })
    add("neoForge", "net.neoforged:neoforge:$NEOFORGE_VERSION")

    implementation("net.caffeinemc:sodium-neoforge:$SODIUM_VERSION")
    implementation("net.caffeinemc:sodium-neoforge-api:$SODIUM_VERSION")
    implementation("net.caffeinemc:sodium-neoforge-mod:$SODIUM_VERSION")
    if (CONTROLIFY_ENABLED) {
        modCompileOnly("dev.isxander:controlify:$CONTROLIFY_VERSION-neoforge") {
            isTransitive = false
        }
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
