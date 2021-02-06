import org.gradle.jvm.tasks.Jar
import java.time.Year

group = "nl.stokpop"
version = "1.1.1"
description = "memory-check"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.30"
    id("org.jetbrains.dokka") version "1.4.20"
    id("com.github.hierynomus.license") version "0.15.0"
    id("com.vanniktech.maven.publish") version "0.13.0"
    // check dependency updates: ./gradlew dependencyUpdates -Drevision=release
    id("com.github.ben-manes.versions") version "0.36.0"
    application
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc-218")
    implementation("com.github.ajalt.clikt:clikt:3.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}

tasks.test {
    useJUnitPlatform()
}

license {
    header = rootProject.file("HEADER")
    ext.set("year",  Year.now().value)
    ext.set("name", "Peter Paul Bakker, Stokpop Software Solutions")
    // keeps complaining about header violations, so added this
    skipExistingHeaders = true
}

application {
    mainClassName = "nl.stokpop.memory.MemoryCheckKt"
}

tasks {
    "assemble" {
        dependsOn(fatJar)
    }
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-exec")
    manifest {
        attributes["Implementation-Title"] = "Stokpop Memory Check"
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = "nl.stokpop.memory.MemoryCheckKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it).matching { exclude { it.name.contains("MANIFEST") } } })
    with(tasks.jar.get() as CopySpec)
}
