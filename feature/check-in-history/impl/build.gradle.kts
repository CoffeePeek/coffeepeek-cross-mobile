import com.coffeepeek.buildlogic.androidApi
import com.coffeepeek.buildlogic.androidImplementation
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}

android {
    namespace = "com.coffeepeek.feature.checkinhistory.impl"
    resourcePrefix = "history_"
}

dependencies {
    implementation(project(module.feature.checkInHistory.domain))
    implementation(project(module.core.presentation))
    implementation(project(module.core.datetime))
    implementation(libs.compose.components.resources)

    androidApi(project(module.feature.checkInHistory.api))
    androidImplementation(project(module.core.designSystem))
    androidImplementation(libs.androidx.lifecycle.viewmodelCompose)
    androidImplementation(libs.androidx.lifecycle.runtimeCompose)
    androidImplementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.tooling)
}
