import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}
android {
    namespace = "com.coffeepeek.feature.attribution.impl"
    resourcePrefix = "attribution_"
}
dependencies {
    api(project(module.feature.attribution.api))
    implementation(project(module.core.designSystem))
    implementation(compose.components.resources)
    implementation(compose.components.uiToolingPreview)
    debugImplementation(libs.compose.ui.tooling)
}
