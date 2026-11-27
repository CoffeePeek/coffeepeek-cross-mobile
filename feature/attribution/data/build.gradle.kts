import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    alias(libs.plugins.kotlin.serialization)
}
android.namespace = "com.coffeepeek.feature.attribution.data"
dependencies {
    api(project(module.feature.attribution.api))
    implementation(project(module.core.network))
    implementation(project(module.core.session))
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
}
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation("com.android.installreferrer:installreferrer:2.2")
        }
    }
}
