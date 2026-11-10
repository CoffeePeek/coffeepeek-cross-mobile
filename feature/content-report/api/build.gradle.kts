import com.coffeepeek.buildlogic.api

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}

android.namespace = "com.coffeepeek.feature.contentreport.api"

dependencies { api(libs.compose.runtime) }
