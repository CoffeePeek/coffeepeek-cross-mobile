package com.coffeepeek.admin.sandbox

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import com.coffeepeek.admin.sandbox.backend.SandboxAuthPolicy
import com.coffeepeek.admin.sandbox.backend.SandboxCities
import com.coffeepeek.admin.sandbox.backend.SandboxContributions
import com.coffeepeek.admin.sandbox.backend.SandboxDiscovery
import com.coffeepeek.admin.sandbox.backend.SandboxFeed
import com.coffeepeek.admin.sandbox.backend.SandboxUpdatePolicy
import com.coffeepeek.admin.sandbox.compose.SandboxScreen
import com.coffeepeek.admin.sandbox.compose.SandboxResourceConfiguration

/** Isolated bootstrap reuses the real graph with local ports, never production services. */
class SandboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) com.coffeepeek.admin.notifications.openNotificationIntent(intent)
        enableEdgeToEdge()
        val fresh = savedInstanceState == null
        setContent {
            val model: SandboxViewModel = viewModel()
            val sandbox = application as SandboxApplication
            // Fixtures that the app caches after its first load (the city catalog) can only be chosen at launch:
            // `am start -n <package>/.sandbox.SandboxActivity --es sandbox_cities Empty --es sandbox_auth WrongPassword --es sandbox_update Available --es sandbox_feed Paged --es sandbox_contributions Paged --es sandbox_discovery Paged`.
            remember {
                if (fresh) {
                    val cities = intent.getStringExtra(EXTRA_CITIES)?.let { runCatching { SandboxCities.valueOf(it) }.getOrNull() }
                    val update = intent.getStringExtra(EXTRA_UPDATE)?.let { runCatching { SandboxUpdatePolicy.valueOf(it) }.getOrNull() }
                    val auth = intent.getStringExtra(EXTRA_AUTH)?.let { runCatching { SandboxAuthPolicy.valueOf(it) }.getOrNull() }
                    val feed = intent.getStringExtra(EXTRA_FEED)?.let { runCatching { SandboxFeed.valueOf(it) }.getOrNull() }
                    feed?.let { sandbox.backend.feed = it }
                    intent.getStringExtra(EXTRA_CONTRIBUTIONS)?.let { runCatching { SandboxContributions.valueOf(it) }.getOrNull() }
                        ?.let { sandbox.backend.contributions = it }
                    intent.getStringExtra(EXTRA_DISCOVERY)?.let { runCatching { SandboxDiscovery.valueOf(it) }.getOrNull() }
                        ?.let { sandbox.backend.discovery = it }
                    cities?.let { sandbox.backend.cities = it }
                    auth?.let { sandbox.backend.authPolicy = it }
                    update?.let { sandbox.backend.updatePolicy = it }
                    model.fixtures(sandbox.backend.updatePolicy, sandbox.backend.cities, sandbox.backend.authPolicy, sandbox.backend.feed, sandbox.backend.contributions, sandbox.backend.discovery)
                }
            }
            SandboxResourceConfiguration(intent.getStringExtra("sandbox_locale")) { SandboxScreen(model, sandbox) }
        }
    }
    private companion object {
        const val EXTRA_CITIES = "sandbox_cities"
        const val EXTRA_UPDATE = "sandbox_update"
        const val EXTRA_AUTH = "sandbox_auth"
        const val EXTRA_FEED = "sandbox_feed"
        const val EXTRA_CONTRIBUTIONS = "sandbox_contributions"
        const val EXTRA_DISCOVERY = "sandbox_discovery"
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        (application as SandboxApplication).blockedExternalAction()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        com.coffeepeek.admin.notifications.openNotificationIntent(intent)
    }
}
