import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "com.coffeepeek.feature.leaderboard.data"

dependencies {
    api(project(module.feature.leaderboard.domain))
    implementation(project(module.core.network))
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
}
