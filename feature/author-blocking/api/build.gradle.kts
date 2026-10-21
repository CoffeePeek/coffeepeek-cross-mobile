import com.coffeepeek.buildlogic.api

plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.feature.authorblocking.api"

dependencies {
    api(libs.kotlinx.coroutines.core)
}
