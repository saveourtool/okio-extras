plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.0.9")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.22.0")
}
