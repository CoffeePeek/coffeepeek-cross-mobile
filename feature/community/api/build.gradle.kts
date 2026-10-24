import com.coffeepeek.buildlogic.androidApi
import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.module

plugins {
    id("com.coffeepeek.kmp.shared-android-compose-library")
}

android.namespace = "com.coffeepeek.feature.community.api"

dependencies {
    api(project(module.feature.community.domain))
    api(project(module.feature.authorBlocking.api))
    api(libs.kotlinx.coroutines.core)
    androidApi(libs.compose.runtime)
    androidApi(compose.ui)
    androidApi(compose.foundation)
    androidApi(compose.material3)
}
