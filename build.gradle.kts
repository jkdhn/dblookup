plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.20.0"
    id("org.jetbrains.changelog") version "2.5.0"
}

group = "me.jkdhn.idea"
version = "1.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.3")
        bundledPlugin("com.intellij.database")
        bundledPlugin("intellij.grid.plugin")
        bundledModule("intellij.grid.impl")
    }
}

intellijPlatform {
    buildSearchableOptions = false
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}

changelog {
    groups = listOf("Improvements", "Fixes")
    repositoryUrl = "https://github.com/jkdhn/dblookup"
}
