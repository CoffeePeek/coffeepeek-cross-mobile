import com.coffeepeek.buildlogic.androidImplementation
import com.coffeepeek.buildlogic.androidInstrumentedTestImplementation
import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.android-compose-library")
}

android {
    namespace = "com.coffeepeek.feature.favorites.impl"
    resourcePrefix = "favorites_"
}

dependencies {
    api(project(module.feature.favorites.api))
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
    androidInstrumentedTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidInstrumentedTestImplementation(libs.androidx.activity.compose)
    androidInstrumentedTestImplementation(libs.androidx.test.runner)
    androidInstrumentedTestImplementation(libs.androidx.espresso.core)
    androidInstrumentedTestImplementation(libs.androidx.navigation3.ui)
    androidInstrumentedTestImplementation(libs.androidx.lifecycle.viewmodelNavigation3)

    debugImplementation(libs.compose.ui.tooling)
}
