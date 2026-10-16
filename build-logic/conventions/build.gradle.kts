plugins {
    `kotlin-dsl`
}

group = "com.coffeepeek"
version = "1.0"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(project(":build-logic-utils"))
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.kotlin.compose.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("kmpAndroidLibrary") {
            id = "com.coffeepeek.kmp.android-library"
            implementationClass = "com.coffeepeek.buildlogic.KmpAndroidLibraryPlugin"
        }
        register("kmpSharedLibrary") {
            id = "com.coffeepeek.kmp.shared-library"
            implementationClass = "com.coffeepeek.buildlogic.KmpSharedLibraryPlugin"
        }
        register("kmpAndroidComposeLibrary") {
            id = "com.coffeepeek.kmp.android-compose-library"
            implementationClass = "com.coffeepeek.buildlogic.KmpAndroidComposeLibraryPlugin"
        }
    }
}
