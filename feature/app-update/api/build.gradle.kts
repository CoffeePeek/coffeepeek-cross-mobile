import com.coffeepeek.buildlogic.androidApi
import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.module

plugins {
    id("com.coffeepeek.kmp.shared-android-compose-library")
}

android.namespace = "com.coffeepeek.feature.appupdate.api"

dependencies {
    api(project(module.feature.appUpdate.domain))
    androidApi(libs.compose.runtime)
}
