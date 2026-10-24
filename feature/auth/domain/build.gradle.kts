import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.feature.auth.domain"

dependencies {
    testImplementation(libs.kotlin.test)
}
