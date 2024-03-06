import com.saveourtool.buildutils.*

plugins {
    kotlin("multiplatform") apply false
    id("com.saveourtool.buildutils.publishing-configuration")
    id("com.saveourtool.diktat") version "2.0.0"
}

group = "com.saveourtool.okio-extras"

allprojects {
    repositories {
        mavenCentral()
    }

    configureDetekt()
}

createDetektTask()
