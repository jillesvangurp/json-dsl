rootProject.name = "json-dsl"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.60.5"
}

refreshVersions {
}
