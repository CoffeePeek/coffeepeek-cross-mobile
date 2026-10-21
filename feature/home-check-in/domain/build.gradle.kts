import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.feature.homecheckin.read.domain"

dependencies {
    testImplementation(libs.kotlin.test)
}
