import com.coffeepeek.buildlogic.androidImplementation
import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-android-compose-library")
}

android {
    namespace = "com.coffeepeek.feature.notifications.impl"
    resourcePrefix = "notifications_"
}

dependencies {
    api(project(module.feature.notifications.api))
    api(project(module.feature.notifications.domain))
    implementation(project(module.core.datetime))
    implementation(libs.kotlinx.coroutines.core)
    androidImplementation(project(module.core.designSystem))
    androidImplementation(libs.androidx.lifecycle.runtimeCompose)
    androidImplementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.tooling)
}
