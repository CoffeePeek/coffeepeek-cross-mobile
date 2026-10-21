import com.coffeepeek.buildlogic.api

plugins {
    id("com.coffeepeek.kmp.android-compose-library")
}

android.namespace = "com.coffeepeek.feature.homecheckin.api"

dependencies {
    api(libs.compose.runtime)
    api(compose.ui)
}
