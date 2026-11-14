import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "com.coffeepeek.feature.settings.data"

dependencies {
    api(project(module.feature.settings.domain))
    implementation(project(module.core.network))
    implementation(project(module.core.session))
    implementation(project(module.core.database))
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
}
