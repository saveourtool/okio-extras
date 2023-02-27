import org.ajoberstar.reckon.core.Scope.MINOR
import org.ajoberstar.reckon.gradle.ReckonExtension
import java.util.Optional

rootProject.name = "okio-extras"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.enterprise") version "3.12.3"
    id("org.ajoberstar.reckon.settings") version "0.17.0-beta.4"
}

configure<ReckonExtension> {
    snapshots()
    setStageCalc(calcStageFromProp())
    setScopeCalc {
        /*
         * MINOR: 1.0.0 -> 1.1.0-SNAPSHOT
         * PATCH: 1.0.0 -> 1.0.1-SNAPSHOT
         */
        Optional.of(MINOR)
    }
}

gradleEnterprise {
    if (System.getenv("CI") != null) {
        buildScan {
            publishAlways()
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}
