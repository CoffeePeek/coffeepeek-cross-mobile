import com.coffeepeek.buildlogic.androidApi
import com.coffeepeek.buildlogic.api

plugins {
    id("com.coffeepeek.kmp.shared-android-compose-library")
}

android.namespace = "com.coffeepeek.feature.roaster.api"

dependencies {
    api(libs.kotlinx.coroutines.core)
    androidApi(libs.compose.runtime)
    androidApi(compose.ui)
    androidApi(compose.foundation)
}
