import com.coffeepeek.buildlogic.androidImplementation
import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-android-compose-library")
}

android {
    namespace = "com.coffeepeek.feature.appupdate.impl"
    resourcePrefix = "appupdate_"
}

dependencies {
    api(project(module.feature.appUpdate.api))
    api(project(module.feature.appUpdate.domain))
    implementation(libs.kotlinx.coroutines.core)
    androidImplementation(project(module.core.designSystem))
    androidImplementation(libs.androidx.lifecycle.runtimeCompose)
    androidImplementation(libs.androidx.core.ktx)
    androidImplementation(libs.androidx.activity.compose)
    androidImplementation("com.google.android.play:app-update:2.1.0")
    androidImplementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.tooling)
}
