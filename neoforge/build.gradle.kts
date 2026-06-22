import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
        inputs.property("version", project.version)

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mapOf("version" to project.version))
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
