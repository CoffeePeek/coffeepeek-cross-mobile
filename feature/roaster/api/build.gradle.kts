import com.coffeepeek.buildlogic.api


plugins {
    id("com.coffeepeek.kmp.shared-library")
}

android.namespace = "com.coffeepeek.feature.roaster.api"

dependencies {
    // Kotlin-only ports. The Compose entries join this module with the UI slice.
    api(libs.kotlinx.coroutines.core)
}
