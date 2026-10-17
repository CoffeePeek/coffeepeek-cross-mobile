import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.feature.homecheckin.impl"

dependencies {
    implementation(project(module.feature.homePreparation.domain))
    implementation(project(module.core.presentation))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
