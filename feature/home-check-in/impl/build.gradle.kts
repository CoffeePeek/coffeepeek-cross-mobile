import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}

android {
    namespace = "com.coffeepeek.feature.homecheckin.impl"
    resourcePrefix = "home_check_in_"
}

dependencies {
    implementation(project(module.feature.homePreparation.domain))
    implementation(project(module.core.presentation))
    implementation(libs.kotlinx.datetime)
    api(project(module.feature.homePreparation.api))
    implementation(project(module.core.designSystem))
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(compose.components.resources)
    implementation(compose.components.uiToolingPreview)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.tooling)
}
