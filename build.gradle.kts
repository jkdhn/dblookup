plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "me.jkdhn.idea"
version = "1.0.7-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.1")
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
