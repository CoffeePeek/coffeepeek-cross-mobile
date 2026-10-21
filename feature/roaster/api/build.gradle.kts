import com.coffeepeek.buildlogic.api

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}

android.namespace = "com.coffeepeek.feature.roaster.api"

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.compose.runtime)
    api(compose.ui)
    api(compose.foundation)
}
