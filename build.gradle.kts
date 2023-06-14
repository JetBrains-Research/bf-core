val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.serialization").version("1.7.22")
    id("org.jlleitschuh.gradle.ktlint") version "11.2.0"
}

group = "org.jetbrains.research.ictl"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-core:$logback_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("net.logstash.logback:logstash-logback-encoder:7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks {
    ktlint {
        ignoreFailures.set(false)
        disabledRules.set(setOf("no-wildcard-imports"))
    }

    test {
        useJUnitPlatform()
        // Show test results.
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed")
        }
    }
}
