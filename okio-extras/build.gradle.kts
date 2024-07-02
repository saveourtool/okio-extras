@file:Suppress(
    "UnstableApiUsage",
    "KDocMissingDocumentation",
)


import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import com.saveourtool.buildutils.*

plugins {
    kotlin("multiplatform")
    id("com.saveourtool.buildutils.publishing-configuration")
}

kotlin {
    jvmToolchain(jdkVersion = 8)

    iosArm64()
    iosSimulatorArm64()
    iosX64()
    jvm()
    linuxX64()
    linuxArm64()
    macosArm64()
    macosX64()
    mingwX64()

    applyDefaultHierarchyTemplate()

    @Suppress(
        "UnusedPrivateMember",
        "UNUSED_VARIABLE",
    )
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.squareup.okio:okio:3.9.0")
            }
        }

        val commonTest by getting {
            dependencies {
                api(kotlin("test"))
                api("io.kotest:kotest-assertions-core:5.9.1")
            }
        }
    }
}

group = "com.saveourtool.okio-extras"

configureSigning()

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
