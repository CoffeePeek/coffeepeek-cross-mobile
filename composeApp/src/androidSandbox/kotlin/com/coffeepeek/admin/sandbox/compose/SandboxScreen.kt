package com.coffeepeek.admin.sandbox.compose

import com.coffeepeek.admin.feature.appupdate.ui.AppUpdateRenderer
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.admin.feature.authorblocking.ui.AuthorBlockingHost
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.koin.compose.koinInject
import com.coffeepeek.R
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.map.LocalMapStyleOverride
import com.coffeepeek.admin.sandbox.SandboxApplication
import com.coffeepeek.admin.sandbox.SandboxViewModel
import com.coffeepeek.admin.sandbox.backend.SandboxAuthPolicy
import com.coffeepeek.admin.sandbox.backend.SandboxCities
import com.coffeepeek.admin.sandbox.backend.SandboxContributions
import com.coffeepeek.admin.sandbox.backend.SandboxDiscovery
import com.coffeepeek.admin.sandbox.backend.SandboxFeed
import com.coffeepeek.admin.sandbox.backend.SandboxScenario
import com.coffeepeek.admin.sandbox.backend.SandboxUpdatePolicy
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.ThemeMode
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.LocalPhotoPickerOverride
import com.coffeepeek.admin.utils.PhotoPickerController
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable internal fun SandboxScreen(model: SandboxViewModel, application: SandboxApplication) {
    val state by model.state.collectAsStateWithLifecycle()
    val updates = koinInject<AppUpdateController>()
    val updateRenderer = koinInject<AppUpdateRenderer>()
    AppContent {
        CompositionLocalProvider(LocalMapStyleOverride provides """{"version":8,"sources":{},"layers":[{"id":"sandbox-background","type":"background","paint":{"background-color":"#716552"}}]}""",
            LocalPhotoPickerOverride provides { limit, deliver ->
            val photo = { if (limit > 0) deliver(listOf(PickedImage(application.photoBytes.copyOf(), "sandbox.png", "image/png"))) }
            PhotoPickerController(photo, photo)
        }) {
            SandboxScreenContent({ model.controls(true) }) {
                key(state.owner) {
                    CompositionLocalProvider(LocalViewModelStoreOwner provides state.owner) { Navigator() }
                }
                // App.kt hosts this next to the Navigator; without it block/unblock confirmations never appear.
                AuthorBlockingHost()
                // Same for the update prompt: App.kt hosts it at the root and it checks on every resume.
                updateRenderer.Prompt()
            }
        }
        if (state.controls) Dialog({ model.controls(false) }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(12.dp)) {
                    SandboxControls(state.scenario, true, { scenario ->
                        application.backend.scenario = scenario; model.refresh(scenario)
                    }, { populated ->
                        application.backend.reset(populated)
                        model.refresh(SandboxScenario.Ready, SandboxUpdatePolicy.UpToDate, SandboxCities.Single, SandboxAuthPolicy.Works, SandboxFeed.Single, SandboxContributions.Seeded, SandboxDiscovery.Seeded)
                        updates.check()
                    }, {
                        ThemeManager.setTheme(if (ThemeManager.themeMode.value == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK)
                    }, state.updatePolicy, { policy ->
                        application.backend.updatePolicy = policy; model.refresh(state.scenario, updatePolicy = policy)
                        updates.check()
                    }, state.cities, { cities ->
                        application.backend.cities = cities; model.refresh(state.scenario, cities = cities)
                    }, state.authPolicy, { policy ->
                        application.backend.authPolicy = policy; model.refresh(state.scenario, authPolicy = policy)
                    }, state.feed, { feed ->
                        application.backend.feed = feed; model.refresh(state.scenario, feed = feed)
                    }, state.contributions, { contributions ->
                        application.backend.contributions = contributions; model.refresh(state.scenario, contributions = contributions)
                    }, state.discovery, { discovery ->
                        application.backend.discovery = discovery; model.refresh(state.scenario, discovery = discovery)
                    })
                    TextButton({ application.sessions.login(); model.refresh(state.scenario) }) { Text(stringResource(R.string.sandbox_login)) }
                    TextButton({ application.sessions.applySession(null); model.refresh(state.scenario) }) { Text(stringResource(R.string.sandbox_guest)) }
                    TextButton({ model.controls(false) }) { Text(stringResource(R.string.sandbox_close)) }
                }
            }
        }
    }
}

@Composable private fun SandboxScreenContent(onControls: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        content()
        SandboxMenuButton(onControls, Modifier.fillMaxSize())
    }
}
@PreviewLightDark @Composable private fun SandboxScreenPreview() = CoffeePeekTheme {
    SandboxScreenContent({}) { Surface(Modifier.fillMaxSize()) {} }
}
