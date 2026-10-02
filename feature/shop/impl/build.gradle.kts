import com.coffeepeek.buildlogic.androidImplementation
import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.android-compose-library")
}

android {
    namespace = "com.coffeepeek.feature.shop.impl"
    resourcePrefix = "shop_"
}

dependencies {
    api(project(module.feature.shop.api))
    implementation(project(module.feature.shop.domain))
    implementation(project(module.feature.favorites.domain))
    implementation(project(module.core.designSystem))
    implementation(project(module.core.presentation))
    implementation(libs.compose.components.resources)
    implementation(libs.compose.components.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.kamel)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    androidImplementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
