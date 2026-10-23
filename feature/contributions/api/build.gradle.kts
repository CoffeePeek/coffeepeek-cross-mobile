import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.module

plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.feature.contributions.api"

dependencies {
    api(project(module.feature.contributions.domain))
    api(libs.kotlinx.coroutines.core)
}
