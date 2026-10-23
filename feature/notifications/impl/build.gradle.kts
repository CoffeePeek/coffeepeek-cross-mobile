import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.feature.notifications.impl"

dependencies {
    api(project(module.feature.notifications.api))
    implementation(project(module.feature.notifications.domain))
    implementation(project(module.feature.notifications.data))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
