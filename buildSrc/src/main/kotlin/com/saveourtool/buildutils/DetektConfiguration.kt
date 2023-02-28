package com.saveourtool.buildutils

import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import java.io.File

private const val PREFIX = "detekt"

fun Project.configureDetekt() {
    tasks.withType<Detekt> {
        parallel = true
        autoCorrect = hasProperty("detektAutoCorrect")
        config.setFrom(file(projectDir / "config" / "detekt" / "detekt.yml"))
        include("**/*.kt")
        reports {
            val reportDir = buildDir / "reports" / "detekt"

            sarif {
                required.set(true)
                outputLocation.set(file(reportDir / "$name.sarif"))
            }

            html {
                required.set(true)
                outputLocation.set(file(reportDir / "$name.html"))
            }

            sequenceOf(xml, txt, md).forEach { report ->
                report.required.set(false)
            }
        }
    }

    detekt("detektCommonMain") {
        dependsOn(tasks.named<KotlinCompile>("compileKotlinJvm"))
    }

    detekt("detektCommonTest") {
        dependsOn(tasks.named<KotlinCompile>("compileTestKotlinJvm"))
    }

    detekt("detektJvmMain") {
        dependsOn(tasks.named<KotlinCompile>("compileKotlinJvm"))
    }

    detekt("detektJvmTest") {
        dependsOn(tasks.named<KotlinCompile>("compileTestKotlinJvm"))
    }

    detekt("detektNativeMain") {
        dependsOn(
            tasks.named<KotlinNativeCompile>("compileKotlinMingwX64"),
            tasks.named<KotlinNativeCompile>("compileKotlinLinuxX64"),
            tasks.named<KotlinNativeCompile>("compileKotlinMacosX64"),
        )
    }

    detekt("detektNativeTest") {
        dependsOn(
            tasks.named<KotlinNativeCompile>("compileTestKotlinMingwX64"),
            tasks.named<KotlinNativeCompile>("compileTestKotlinLinuxX64"),
            tasks.named<KotlinNativeCompile>("compileTestKotlinMacosX64"),
        )
    }

    tasks.register<DefaultTask>("detektAll") {
        dependsOn(
            tasks.named<Detekt>("detektCommonMain"),
            tasks.named<Detekt>("detektCommonTest"),
            tasks.named<Detekt>("detektJvmMain"),
            tasks.named<Detekt>("detektJvmTest"),
            tasks.named<Detekt>("detektNativeMain"),
            tasks.named<Detekt>("detektNativeTest"),
        )
    }
}

/**
 * Configures a _Detekt_ task, creating it if necessary.
 */
private fun Project.detekt(
    name: String,
    configuration: Detekt.() -> Unit = {},
) {
    val taskProvider = when (tasks.findByName(name)) {
        null -> tasks.register<Detekt>(name)
        else -> tasks.named<Detekt>(name)
    }

    taskProvider {
        source = fileTree(projectDir / "src" / name.sourceSetName)
    }
    taskProvider(configuration)
}

private val String.sourceSetName: String
    get() {
        val suffix = when {
            startsWith(PREFIX) -> substring(PREFIX.length).decapitalize()
            else -> null
        }

        return when {
            suffix.isNullOrEmpty() -> this
            else -> suffix
        }
    }

private operator fun File.div(relative: String): File =
    resolve(relative)
