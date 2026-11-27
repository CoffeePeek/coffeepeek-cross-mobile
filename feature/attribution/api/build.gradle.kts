import com.coffeepeek.buildlogic.api
import com.coffeepeek.buildlogic.module

plugins { id("com.coffeepeek.kmp.shared-library") }
android.namespace = "com.coffeepeek.feature.attribution.api"
dependencies { api(project(module.feature.attribution.domain)) }
