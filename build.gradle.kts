@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    kotlin("multiplatform") version "1.8.10"
    eclipse
    `maven-publish`
}

group = "com.saveourtool"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    /*
     * For Kotlin/MPP, `mavenLocal()` should never be the 1st repository, as
     * dependency resolution for non-JVM targets may fail.
     *
     * Yet, it may be useful if your project has snapshot dependencies
     * (publishing to ~/.m2/ is unaffected by the presence of this call).
     */
    mavenLocal()
}

kotlin {
    jvm {
        withJava()
    }
    jvmToolchain(jdkVersion = 17)

    val hostOs = DefaultNativePlatform.getCurrentOperatingSystem()
    val nativeTarget = when {
        hostOs.isWindows -> mingwX64()
        hostOs.isLinux -> linuxX64()
        hostOs.isMacOsX -> macosX64()
        else -> throw GradleException("Host OS $hostOs is not supported.")
    }

    @Suppress(
        "UnusedPrivateMember",
        "UNUSED_VARIABLE",
    )
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.squareup.okio:okio:3.3.0")
            }
        }

        val commonTest by getting {
            dependencies {
                api(kotlin("test"))
                api("io.kotest:kotest-assertions-core:5.5.5")
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }

        val nativeTest by creating {
            dependsOn(commonTest)
        }

        val jvmMain by getting

        val jvmTest by getting

        sequenceOf(
            mingwX64(),
            linuxX64(),
            macosX64(),
        ).forEach { target ->
            getByName("${target.name}Main").dependsOn(nativeMain)
            getByName("${target.name}Test").dependsOn(nativeTest)
        }
    }
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
        showCauses = true
        showExceptions = true
        showStackTraces = true
        exceptionFormat = FULL
        events("passed", "skipped")
    }
}

publishing {
    repositories {
        maven {
            name = "GitHub"
            url = uri("https://maven.pkg.github.com/saveourtool/okio-extras")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
