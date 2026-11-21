import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.attributes.Attribute

plugins {
    id("idea")
    id("java-library")
    id("dev.architectury.loom-no-remap")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val MINECRAFT_VERSION = rootProject.extra["MINECRAFT_VERSION"] as String
val NEOFORGE_VERSION = rootProject.extra["NEOFORGE_VERSION"] as String
val SODIUM_VERSION = rootProject.extra["SODIUM_VERSION"] as String
val CONTROLIFY_VERSION = rootProject.extra["CONTROLIFY_VERSION"] as String
val CONTROLIFY_ENABLED = rootProject.extra["CONTROLIFY_ENABLED"] as Boolean
val CONTROLIFY_LOADER_ATTRIBUTE = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)

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
    minecraft("net.minecraft:minecraft:$MINECRAFT_VERSION")
    add("neoForge", "net.neoforged:neoforge:$NEOFORGE_VERSION")

    implementation("net.caffeinemc:sodium-neoforge:$SODIUM_VERSION")
    implementation("net.caffeinemc:sodium-neoforge-api:$SODIUM_VERSION")
    implementation("net.caffeinemc:sodium-neoforge-mod:$SODIUM_VERSION")
    if (CONTROLIFY_ENABLED) {
        compileOnly("dev.isxander:controlify:$CONTROLIFY_VERSION") {
            isTransitive = false
            attributes {
                attribute(CONTROLIFY_LOADER_ATTRIBUTE, "neoforge")
            }
        }
    }
    add("common", project(":common")) {
        isTransitive = false
    }
    add("shadowBundle", project(path = ":common", configuration = "runtimeElements")) {
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
            "configureLaunch",
            "configureClientLaunch",
        )
        classpath += configurations.named("runtimeClasspath").get()
    }
}

tasks {
    processResources {
        val modVersion = project.version
        inputs.property("version", modVersion)
        inputs.property("minecraft_version", MINECRAFT_VERSION)
        inputs.property("sodium_version", SODIUM_VERSION)
        inputs.property("controlifyEnabled", CONTROLIFY_ENABLED)

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mapOf(
                "version" to modVersion,
                "minecraft_version" to MINECRAFT_VERSION,
                "sodium_version" to SODIUM_VERSION,
            ))
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
