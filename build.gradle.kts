@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io") {
        content {
            includeGroup("com.github.jillesvangurp")
        }
    }
}

kotlin {
    jvm {
        // should work for android as well
    }
    js(IR) {
        nodejs {
            testTask {
                useMocha {
                    // javascript is a lot slower than Java, we hit the default timeout of 2000
                    timeout = "60s"
                }
            }
        }
    }
    linuxX64()
    linuxArm64()
    mingwX64()
    macosX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    iosSimulatorArm64()
    wasmJs {
        browser()
        nodejs()
        d8()
    }
    // blocked on kotest assertions wasm release
//    wasmWasi()

    sourceSets {

        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("io.kotest:kotest-assertions-core:_")
            }
        }

        jvmMain  {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        jvmTest {
            dependencies {
                implementation("com.github.jillesvangurp:kotlin4example:_")
                runtimeOnly("org.junit.jupiter:junit-jupiter:_")
                implementation(kotlin("test-junit"))
            }
        }

        jsMain  {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        jsTest  {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        wasmJsTest {
            dependencies {
                implementation(kotlin("test-wasm-js"))
            }
        }

        all {
            languageSettings {
                languageVersion = "1.9"
                apiVersion = "1.9"
            }
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
    }
}

tasks.named("iosSimulatorArm64Test") {
    // requires IOS simulator and tens of GB of other stuff to be installed
    // so keep it disabled
    enabled = false
}

publishing {
    repositories {
        maven {
            // GOOGLE_APPLICATION_CREDENTIALS env var must be set for this to work
            // public repository is at https://maven.tryformation.com/releases
            url = uri("gcs://mvn-public-tryformation/releases")
            name = "FormationPublic"
        }
    }
}



