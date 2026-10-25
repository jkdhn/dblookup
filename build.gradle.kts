plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "me.jkdhn.idea"
version = "1.0.8"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("263.7307.44")
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
