plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.0.15")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.0")
}
