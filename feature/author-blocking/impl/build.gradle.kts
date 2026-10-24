import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.feature.authorblocking.impl"

dependencies {
    api(project(module.feature.authorBlocking.api))
    api(libs.ktor.client.core)
    implementation(project(module.feature.authorBlocking.domain))
    implementation(project(module.feature.authorBlocking.data))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
