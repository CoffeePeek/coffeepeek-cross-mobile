import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}

android.namespace = "com.coffeepeek.feature.ownership.impl"

dependencies {
    api(project(":feature:business-ownership:api"))
    implementation(project(":feature:business-ownership:domain"))
    implementation(project(":core:design-system"))
    implementation(project(":core:presentation"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.compose.ui.backhandler)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(compose.components.resources)
    implementation(compose.components.uiToolingPreview)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
