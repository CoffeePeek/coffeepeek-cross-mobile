import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.module

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}

android.namespace = "com.coffeepeek.feature.appupdate.api"

dependencies {
    api(project(module.feature.appUpdate.domain))
    api(libs.compose.runtime)
}
