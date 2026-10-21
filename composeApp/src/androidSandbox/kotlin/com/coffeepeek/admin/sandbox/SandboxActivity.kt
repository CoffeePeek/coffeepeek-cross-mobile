package com.coffeepeek.admin.sandbox

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import com.coffeepeek.admin.sandbox.backend.SandboxCities
import com.coffeepeek.admin.sandbox.backend.SandboxUpdatePolicy
import com.coffeepeek.admin.sandbox.compose.SandboxScreen

/** Isolated bootstrap reuses the real graph with local ports, never production services. */
class SandboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val fresh = savedInstanceState == null
        setContent {
            val model: SandboxViewModel = viewModel()
            val sandbox = application as SandboxApplication
            // Fixtures that the app caches after its first load (the city catalog) can only be chosen at launch:
            // `am start -n <package>/.sandbox.SandboxActivity --es sandbox_cities Empty --es sandbox_update Available`.
            remember {
                if (fresh) {
                    val cities = intent.getStringExtra(EXTRA_CITIES)?.let { runCatching { SandboxCities.valueOf(it) }.getOrNull() }
                    val update = intent.getStringExtra(EXTRA_UPDATE)?.let { runCatching { SandboxUpdatePolicy.valueOf(it) }.getOrNull() }
                    cities?.let { sandbox.backend.cities = it }
                    update?.let { sandbox.backend.updatePolicy = it }
                    model.fixtures(sandbox.backend.updatePolicy, sandbox.backend.cities)
                }
            }
            SandboxScreen(model, sandbox)
        }
    }
    private companion object {
        const val EXTRA_CITIES = "sandbox_cities"
        const val EXTRA_UPDATE = "sandbox_update"
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        (application as SandboxApplication).blockedExternalAction()
    }
}
