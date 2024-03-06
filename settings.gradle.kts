import org.ajoberstar.reckon.core.Reckoner
import org.ajoberstar.reckon.core.ScopeCalculator
import org.ajoberstar.reckon.core.VersionTagParser
import org.ajoberstar.reckon.gradle.ReckonExtension

rootProject.name = "okio-extras"

plugins {
    id("org.ajoberstar.reckon.settings") version "0.18.3"
}

includeBuild("gradle/plugins")
include("okio-extras")

extensions.configure<ReckonExtension> {
    setDefaultInferredScope("patch")
    // to activate release, provide `-Prelease` or `-Prelease=true`. To deactivate, either omit the property, or set `-Prelease=false`.
    val isRelease = extra.has("release") && extra["release"] != "false"
    if (isRelease) {
        val scopeCalculator = ScopeCalculator { inventory ->
            if (inventory.isClean) {
                calcScopeFromProp().calculate(inventory)
            } else {
                throw GradleException(
                    "Release build will be performed with not clean git tree; aborting."
                )
            }
        }
        setScopeCalc(scopeCalculator)
    } else {
        setScopeCalc(calcScopeFromProp())
    }
    val isSnapshot = extra.has("reckon.stage") && extra["reckon.stage"] == "snapshot"
    if (isSnapshot) {
        // we should build snapshots only for snapshot publishing, so it requires explicit parameter
        snapshots()
    } else {
        stages("beta", "rc", Reckoner.FINAL_STAGE)
    }
    setStageCalc(calcStageFromProp())

    // A terrible hack to remove all pre-release tags. Because in semver `0.1.0-SNAPSHOT` < `0.1.0-alpha`, in snapshot mode
    // we remove tags like `0.1.0-alpha`, and then reckoned version will still be `0.1.0-SNAPSHOT` and it will be compliant.
    val tagParser = VersionTagParser { tag: String ->
        if (tag.matches(Regex("""^v\d+\.\d+\.\d+$"""))) {
            VersionTagParser.getDefault().parse(tag)
        } else {
            java.util.Optional.empty()
        }
    }
    setTagParser(tagParser)
}
