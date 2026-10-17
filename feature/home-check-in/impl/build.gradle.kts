import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.androidApi
import com.coffeepeek.buildlogic.androidImplementation
import com.coffeepeek.buildlogic.androidInstrumentedTestImplementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-android-compose-library")
}

android {
    namespace = "com.coffeepeek.feature.homecheckin.impl"
    resourcePrefix = "home_check_in_"
}

dependencies {
    implementation(project(module.feature.homePreparation.domain))
    implementation(project(module.core.presentation))
    androidApi(project(module.feature.homePreparation.api))
    androidImplementation(project(module.core.designSystem))
    androidImplementation(libs.androidx.lifecycle.viewmodelCompose)
    androidImplementation(libs.androidx.lifecycle.runtimeCompose)
    androidImplementation(libs.androidx.activity.compose)
    androidImplementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    androidInstrumentedTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidInstrumentedTestImplementation(libs.androidx.activity.compose)
    androidInstrumentedTestImplementation(libs.androidx.test.runner)
    androidInstrumentedTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.compose.ui.tooling)
}
