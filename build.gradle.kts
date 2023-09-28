val kotlin_version: String by project
val logback_version: String by project

plugins {
  kotlin("jvm") version "1.8.20"
  kotlin("plugin.serialization").version("1.8.20")
  `maven-publish`

  id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
}

group = "org.jetbrains.research.ictl"
version = "0.0.7"

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
  testImplementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
}

tasks {
  compileJava {
    targetCompatibility = "1.8"
  }
  compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
  }

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

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = group.toString()
      artifactId = "bf-core"
      version = version
      from(components["java"])
    }
  }
  repositories {
    maven {
      url = uri("https://packages.jetbrains.team/maven/p/ictl-public/public-maven")
      credentials {
        username = System.getenv("MAVEN_USERNAME")
        password = System.getenv("MAVEN_PASSWORD")
      }
    }
  }
}
