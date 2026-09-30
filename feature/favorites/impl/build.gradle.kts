import com.coffeepeek.buildlogic.module
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("com.coffeepeek.kmp.android-compose-library")
}

coffeepeekModule {
    androidNamespace = "com.coffeepeek.feature.favorites.impl"
    resourcePrefix = "favorites_"
}

extensions.configure<KotlinMultiplatformExtension> {
    sourceSets {
        commonMain.dependencies {
            api(project(module.feature.favorites.api))
            implementation(project(module.feature.favorites.domain))
            implementation(project(module.core.designSystem))
            implementation(project(module.core.presentation))
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kamel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.compose.ui.tooling.preview)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.espresso.core)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
        }
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
}
