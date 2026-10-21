import com.coffeepeek.buildlogic.api

plugins {
    id("com.coffeepeek.kmp.android-compose-library")
}

android.namespace = "com.coffeepeek.feature.favorites.api"

dependencies {
    api(libs.compose.runtime)
    api(compose.ui)
}
