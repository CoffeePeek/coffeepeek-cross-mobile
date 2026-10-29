import com.coffeepeek.buildlogic.androidImplementation
import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.module
import com.coffeepeek.buildlogic.testImplementation

plugins {
    id("com.coffeepeek.kmp.shared-library")
    alias(libs.plugins.kotlin.serialization)
}

android.namespace = "com.coffeepeek.feature.shelf.data"

dependencies {
    api(project(module.feature.shelf.domain))
    implementation(project(module.core.network))
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    // Scanner models (U2-Net-p, MI-GAN) run on ONNX Runtime: the Android AAR here, the Swift package on iOS below.
    androidImplementation(libs.onnxruntime.android)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
}

kotlin {
    // Model downloads on iOS use a plain client of their own: never the API client with its session headers.
    sourceSets.iosMain.dependencies { implementation(libs.ktor.client.darwin) }
    swiftPMDependencies {
        // ONNX Runtime's Objective-C API for iosMain, through a local package that adds the linker settings it lacks.
        localSwiftPackage(
            directory = layout.projectDirectory.dir("ShelfOnnxRuntime"),
            products = listOf("ShelfOnnxRuntime"),
            importedClangModules = listOf("OnnxRuntimeBindings"),
        )
    }
}
