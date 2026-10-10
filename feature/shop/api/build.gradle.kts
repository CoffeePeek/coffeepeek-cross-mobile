import com.coffeepeek.buildlogic.api

plugins {
    id("com.coffeepeek.kmp.android-compose-library")
}

android.namespace = "com.coffeepeek.feature.shop.api"

dependencies {
    api(libs.compose.runtime)
}
