import com.coffeepeek.buildlogic.api

plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.feature.authorblocking.domain"

dependencies {
    api(libs.kotlinx.coroutines.core)
}
