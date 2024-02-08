@file:Suppress(
    "UnstableApiUsage",
    "KDocMissingDocumentation",
)

import com.saveourtool.buildutils.configureDetekt
import org.ajoberstar.reckon.gradle.ReckonCreateTagTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutput.Style.Failure
import org.gradle.internal.logging.text.StyledTextOutput.Style.Success
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
    kotlin("multiplatform")
    eclipse
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.9.10"
    id("io.gitlab.arturbosch.detekt")
    id("com.saveourtool.diktat") version "2.0.0"
    id("com.saveourtool.buildutils.publishing-configuration")
}

group = "com.saveourtool"
description = "A set of extensions to Okio"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    jvmToolchain(jdkVersion = 8)

    mingwX64()
    linuxX64()
    macosX64()
    macosArm64()

    @Suppress(
        "UnusedPrivateMember",
        "UNUSED_VARIABLE",
    )
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.squareup.okio:okio:3.7.0")
            }
        }

        val commonTest by getting {
            dependencies {
                api(kotlin("test"))
                api("io.kotest:kotest-assertions-core:5.8.0")
            }
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

/*
 * This is expected to work some day (but currently it does not),
 * see https://youtrack.jetbrains.com/issue/KT-32608.
 */
tasks.withType<KotlinTest> {
    reports.junitXml.required.set(true)
}

tasks.withType<Test> {
    reports.junitXml.required.set(true)
}

tasks.withType<AbstractPublishToMaven> {
    dependsOn(tasks.withType<Sign>())
}

tasks.withType<ReckonCreateTagTask> {
    dependsOn(tasks.check)
}

configureDetekt()


fun Project.styledOut(logCategory: String): StyledTextOutput =
    serviceOf<StyledTextOutputFactory>().create(logCategory)

/**
 * Determines if this project has all the given properties.
 *
 * @param propertyNames the names of the properties to locate.
 * @return `true` if this project has all the given properties, `false` otherwise.
 * @see Project.hasProperty
 */
fun Project.hasProperties(vararg propertyNames: String): Boolean =
    propertyNames.asSequence().all(this::hasProperty)
