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

/**
 * Configures _Detekt_ for this project.
 */
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

    detekt("detektCommonMain")
    detekt("detektCommonTest")
    detekt("detektJvmMain")
    detekt("detektJvmTest")
    detekt("detektNativeMain")
    detekt("detektNativeTest")

    tasks.register<DefaultTask>("detektAll") {
        group = "verification"

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
 *
 * @param configuration extra configuration, may be empty.
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
        val sourceSetName = name.sourceSetName

        source = fileTree(projectDir / "src" / sourceSetName)

        val isTest = sourceSetName.endsWith("Test")
        val dependencyNamePrefix = when {
            isTest -> "compileTestKotlin"
            else -> "compileKotlin"
        }

        val isNative = sourceSetName.startsWith("native")
        when {
            isNative -> dependsOn(
                tasks.named<KotlinNativeCompile>(dependencyNamePrefix + "MingwX64"),
                tasks.named<KotlinNativeCompile>(dependencyNamePrefix + "LinuxX64"),
                tasks.named<KotlinNativeCompile>(dependencyNamePrefix + "MacosX64"),
            )

            else -> dependsOn(tasks.named<KotlinCompile>(dependencyNamePrefix + "Jvm"))
        }
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
