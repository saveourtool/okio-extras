plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.3.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.5")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.18.2")
    implementation("org.ajoberstar.grgit:grgit-core:4.1.0")
}
