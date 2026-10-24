import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}

android {
    namespace = "com.coffeepeek.feature.profile.impl"
    resourcePrefix = "profile_"
}

dependencies {
    api(project(module.feature.profile.api))
    implementation(project(module.feature.profile.domain))
    implementation(project(module.core.presentation))
    implementation(project(module.core.datetime))
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(module.core.designSystem))
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(compose.components.resources)
    implementation(compose.components.uiToolingPreview)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.tooling)
}
