package com.saveourtool.buildutils

import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

fun Project.configureDetekt() {
    tasks.withType<Detekt> {
        parallel = true
        autoCorrect = hasProperty("detektAutoCorrect")
        config.setFrom(file(projectDir.resolve("config").resolve("detekt").resolve("detekt.yml")))
        include("**/*.kt")
        reports {
            val reportDir = buildDir.resolve("reports").resolve("detekt")

            sarif {
                required.set(true)
                outputLocation.set(file(reportDir.resolve("$name.sarif")))
            }

            html {
                required.set(true)
                outputLocation.set(file(reportDir.resolve("$name.html")))
            }

            sequenceOf(xml, txt, md).forEach { report ->
                report.required.set(false)
            }
        }
    }

    tasks.register<Detekt>("detektCommonMain") {
        dependsOn(tasks.named<KotlinCompile>("compileKotlinJvm"))
        source = fileTree(projectDir.resolve("src").resolve("commonMain"))
    }

    tasks.register<Detekt>("detektCommonTest") {
        dependsOn(tasks.named<KotlinCompile>("compileTestKotlinJvm"))
        source = fileTree(projectDir.resolve("src").resolve("commonTest"))
    }

    tasks.named<Detekt>("detektJvmMain") {
        dependsOn(tasks.named<KotlinCompile>("compileKotlinJvm"))
        source = fileTree(projectDir.resolve("src").resolve("jvmMain"))
    }

    tasks.named<Detekt>("detektJvmTest") {
        dependsOn(tasks.named<KotlinCompile>("compileTestKotlinJvm"))
        source = fileTree(projectDir.resolve("src").resolve("jvmTest"))
    }

    tasks.register<Detekt>("detektNativeMain") {
        dependsOn(
            tasks.named<KotlinNativeCompile>("compileKotlinMingwX64"),
            tasks.named<KotlinNativeCompile>("compileKotlinLinuxX64"),
            tasks.named<KotlinNativeCompile>("compileKotlinMacosX64"),
        )
        source = fileTree(projectDir.resolve("src").resolve("nativeMain"))
    }

    tasks.register<Detekt>("detektNativeTest") {
        dependsOn(
            tasks.named<KotlinNativeCompile>("compileTestKotlinMingwX64"),
            tasks.named<KotlinNativeCompile>("compileTestKotlinLinuxX64"),
            tasks.named<KotlinNativeCompile>("compileTestKotlinMacosX64"),
        )
        source = fileTree(projectDir.resolve("src").resolve("nativeTest"))
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
