[![JetBrains Research](https://jb.gg/badges/research.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Java CI with Gradle](https://github.com/JetBrains-Research/bf-core/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/JetBrains-Research/bf-core/actions/workflows/ci.yml)
# `bf-core`

## Overview
bf-core is available from the following repository: `https://packages.jetbrains.team/maven/p/ictl-public/public-maven`

### Gradle
Add a Maven repository to the build.gradle.kts file:
```kotlin
repositories {
    ...
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ictl-public/public-maven")
    }
    ...
}

dependencies { 
    ...
    implementation("org.jetbrains.research.ictl:bf-core:$version")
    ...
}
```

### Maven
Add a repository to the pom.xml file:
```xml
<repositories>
  <repository>
    <id>space-public-maven</id>
    <url>https://packages.jetbrains.team/maven/p/ictl-public/public-maven</url>
  </repository>
</repositories>

<dependencies>
  ...
  <dependency>
    <groupId>org.jetbrains.research.ictl</groupId>
    <artifactId>bf-core</artifactId>
    <version>${version}</version>
  </dependency>
  ...
</dependencies>
```

