plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "me.jkdhn.idea"
version = "1.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2025.1")
        bundledPlugin("com.intellij.database")
        bundledPlugin("intellij.grid.plugin")
        bundledModule("intellij.grid.impl")
    }
}

intellijPlatform {
    buildSearchableOptions = false
    pluginVerification {
        ides {
            recommended()
        }
    }
}
