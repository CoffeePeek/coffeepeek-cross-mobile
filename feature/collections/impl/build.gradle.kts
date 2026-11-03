import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}
android {
    namespace = "com.coffeepeek.feature.collections.impl"
    resourcePrefix = "collections_"
}
dependencies {
    api(project(module.feature.collections.api))
    implementation(project(module.feature.collections.domain))
    implementation(project(module.core.designSystem))
    implementation(project(module.core.presentation))
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.kamel)
    implementation(libs.compose.ui.backhandler)
    implementation(libs.qrose)
    implementation(compose.components.resources)
    implementation(compose.components.uiToolingPreview)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.tooling)
}
