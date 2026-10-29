import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.module

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}

android.namespace = "com.coffeepeek.feature.discovery.api"

dependencies {
    api(project(module.feature.discovery.domain))
    api(libs.kotlinx.coroutines.core)
    api(libs.compose.runtime)
    api(compose.ui)
    api(compose.foundation)
}
