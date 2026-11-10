import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "com.coffeepeek.feature.shop.data"

dependencies {
    api(project(module.feature.shop.domain))
    implementation(project(module.feature.shop.api))
    implementation(project(module.core.network))
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation("com.squareup.okio:okio:3.16.0")

    testImplementation(libs.kotlin.test)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
}
