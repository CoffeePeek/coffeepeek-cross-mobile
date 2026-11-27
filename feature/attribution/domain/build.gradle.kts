import com.coffeepeek.buildlogic.implementation
import com.coffeepeek.buildlogic.testImplementation

plugins { id("com.coffeepeek.kmp.shared-library") }
android.namespace = "com.coffeepeek.feature.attribution.domain"
dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
