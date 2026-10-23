import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "com.coffeepeek.feature.notifications.data"

dependencies {
    api(project(module.feature.notifications.domain))
    api(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(module.core.network))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
}
