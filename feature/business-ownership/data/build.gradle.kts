import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "com.coffeepeek.feature.ownership.data"

dependencies {
    api(project(":feature:business-ownership:domain"))
    implementation(project(":core:network"))
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
