import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.core.datetime"

dependencies {
    testImplementation(libs.kotlin.test)
}
