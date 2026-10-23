import com.coffeepeek.buildlogic.androidApi

plugins {
    id("com.coffeepeek.kmp.shared-android-compose-library")
}

android.namespace = "com.coffeepeek.feature.auth.api"

dependencies {
    androidApi(libs.compose.runtime)
    androidApi(compose.ui)
}
