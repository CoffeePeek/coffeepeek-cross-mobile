import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    id("com.coffeepeek.kmp.android-compose-library")
}

android.namespace = "com.coffeepeek.feature.ownership.api"

dependencies {
    api(compose.runtime)
    api(compose.ui)
    api(compose.foundation)
    testImplementation(libs.kotlin.test)
}
