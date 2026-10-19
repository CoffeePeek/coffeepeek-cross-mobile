import com.coffeepeek.buildlogic.androidImplementation
import com.coffeepeek.buildlogic.androidInstrumentedTestImplementation
import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-android-compose-library")
}

android {
    namespace = "com.coffeepeek.feature.roaster.impl"
    resourcePrefix = "roaster_"
}

dependencies {
    api(project(module.feature.roaster.api))
    implementation(project(module.feature.roaster.domain))
    implementation(project(module.core.presentation))
    implementation(libs.kotlinx.coroutines.core)
    androidImplementation(project(module.core.designSystem))
    androidImplementation(libs.androidx.lifecycle.viewmodelCompose)
    androidImplementation(libs.androidx.lifecycle.runtimeCompose)
    androidImplementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    androidInstrumentedTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidInstrumentedTestImplementation(libs.androidx.activity.compose)
    androidInstrumentedTestImplementation(libs.androidx.test.runner)
    androidInstrumentedTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.compose.ui.tooling)
}
