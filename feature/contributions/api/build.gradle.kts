import com.coffeepeek.buildlogic.androidApi
import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.module

plugins {
    id("com.coffeepeek.kmp.shared-android-compose-library")
}

android.namespace = "com.coffeepeek.feature.contributions.api"

dependencies {
    api(project(module.feature.contributions.domain))
    api(libs.kotlinx.coroutines.core)
    androidApi(libs.compose.runtime)
    androidApi(compose.ui)
}
