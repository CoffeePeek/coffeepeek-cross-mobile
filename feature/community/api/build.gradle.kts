import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.module

plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.feature.community.api"

dependencies {
    api(project(module.feature.community.domain))
    api(libs.kotlinx.coroutines.core)
}
