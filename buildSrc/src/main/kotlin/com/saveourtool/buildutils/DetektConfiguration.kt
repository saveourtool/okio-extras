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
        source = fileTree(projectDir / "src" / "commonMain")
    }

    detekt("detektCommonTest") {
        dependsOn(tasks.named<KotlinCompile>("compileTestKotlinJvm"))
        source = fileTree(projectDir / "src" / "commonTest")
    }

    detekt("detektJvmMain") {
        dependsOn(tasks.named<KotlinCompile>("compileKotlinJvm"))
        source = fileTree(projectDir / "src" / "jvmMain")
    }

    detekt("detektJvmTest") {
        dependsOn(tasks.named<KotlinCompile>("compileTestKotlinJvm"))
        source = fileTree(projectDir / "src" / "jvmTest")
    }

    detekt("detektNativeMain") {
        dependsOn(
            tasks.named<KotlinNativeCompile>("compileKotlinMingwX64"),
            tasks.named<KotlinNativeCompile>("compileKotlinLinuxX64"),
            tasks.named<KotlinNativeCompile>("compileKotlinMacosX64"),
        )
        source = fileTree(projectDir / "src" / "nativeMain")
    }

    detekt("detektNativeTest") {
        dependsOn(
            tasks.named<KotlinNativeCompile>("compileTestKotlinMingwX64"),
            tasks.named<KotlinNativeCompile>("compileTestKotlinLinuxX64"),
            tasks.named<KotlinNativeCompile>("compileTestKotlinMacosX64"),
        )
        source = fileTree(projectDir / "src" / "nativeTest")
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
    configuration: Detekt.() -> Unit,
) =
    when (tasks.findByName(name)) {
        null -> tasks.register<Detekt>(name)
        else -> tasks.named<Detekt>(name)
    }(configuration)

private operator fun File.div(relative: String): File =
    resolve(relative)
