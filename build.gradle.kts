plugins {
    kotlin("jvm") version "2.1.20-RC"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.papermc.paperweight.userdev") version "1.7.3"
    id("maven-publish")
}

group = "com.teddeh"
version = "1.0.0"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "$group"
            artifactId = project.name.lowercase()
            version = version

            artifact(tasks.shadowJar) {
                classifier = null
            }
        }
    }
    repositories {
        maven {
            url = uri("file://${System.getProperty("user.home")}/.m2/repository") // Default local Maven repo
        }
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc-repo" }
    maven("https://oss.sonatype.org/content/groups/public/") { name = "sonatype" }
    maven(url = "https://mvn.lumine.io/repository/maven-public/") { name = "lumine" }
    maven(url = "https://repo.codemc.io/repository/maven-releases/") { name = "codemc" }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    paperweight.paperDevBundle("1.21.3-R0.1-SNAPSHOT")

    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.4")
    compileOnly("com.github.retrooper:packetevents-spigot:2.8.0")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

