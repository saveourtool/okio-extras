plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.1.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.0")
}
