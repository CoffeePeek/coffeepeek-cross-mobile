import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.androidImplementation
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "com.coffeepeek.core.session"

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    androidImplementation("androidx.security:security-crypto:1.0.0")

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
