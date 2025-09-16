import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.jvm.tasks.Jar
import java.time.Year

group = "nl.stokpop"
version = file("VERSION").readText().trim()
description = "memory-check"

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jetbrains.dokka") version "2.0.0"
    id("com.github.hierynomus.license") version "0.16.1"
    id("com.vanniktech.maven.publish") version "0.34.0"
    // check dependency updates: ./gradlew dependencyUpdates -Drevision=release
    id("com.github.ben-manes.versions") version "0.52.0"
    id("org.sonarqube") version "5.1.0.4882"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.2.20"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.25")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
}

tasks.test {
    useJUnitPlatform()
}

license {
    header = rootProject.file("HEADER")
    ext.set("year",  Year.now().value)
    ext.set("name", "Peter Paul Bakker, Stokpop Software Solutions")
    exclude("*.txt")
    // keeps complaining about header violations, so added this
    skipExistingHeaders = true
}

application {
    mainClass.set("nl.stokpop.memory.MemoryCheckKt")
}

val fatJar by tasks.registering(Jar::class) {
    archiveBaseName.set("${project.name}-exec")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Implementation-Title"] = "Stokpop Memory Check"
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = "nl.stokpop.memory.MemoryCheckKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it).matching { exclude { it.name.contains("MANIFEST") } } })
    with(tasks.named<Jar>("jar").get() as CopySpec)
}

tasks.named("assemble") {
    dependsOn(fatJar)
}

// prevent alpha releases as a suggestion to update in dependencyUpdates
// see: https://github.com/ben-manes/gradle-versions-plugin#revisions
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

