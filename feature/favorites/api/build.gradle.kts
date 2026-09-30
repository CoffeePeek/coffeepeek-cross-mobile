import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("com.coffeepeek.kmp.android-compose-library")
    alias(libs.plugins.kotlin.serialization)
}

coffeepeekModule {
    androidNamespace = "com.coffeepeek.feature.favorites.api"
}

extensions.configure<KotlinMultiplatformExtension> {
    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.androidx.navigation3.runtime)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
