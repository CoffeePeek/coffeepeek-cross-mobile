package com.coffeepeek.admin.sandbox.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.coffeepeek.R
import com.coffeepeek.admin.sandbox.SandboxViewModel
import com.coffeepeek.admin.sandbox.backend.SandboxScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable internal fun SandboxScreen(model: SandboxViewModel, onFinish: () -> Unit, onThemeApplied: (Boolean) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    SideEffect { onThemeApplied(state.dark) }
    CoffeePeekTheme(darkTheme = state.dark) {
        SandboxScreenContent(state.scenario, state.destination == null, model::scenario, model::reset, model::theme) {
            key(state.historyOwner) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides state.historyOwner) {
                    model.environment.history.Content(state.generation, true, { value -> value.take(10) }, onFinish,
                        { model.scope(true) }, { model.scope(true) }, model::photos,
                        { value -> runCatching { DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                            .format(Instant.parse(value)) }.getOrDefault(value) },
                        { id -> model.open(id) }, { model.scope(true) }, { model.open() }, state.generation)
                }
            }
        }
        state.destination?.let { SandboxHomeOverlay(model, it, state.dismissAllowed, state.generation) }
        state.photos?.let { (urls, index) -> SandboxPhotoViewer(model.environment.backend, urls, index, model::closePhotos) }
        if (state.showScope) AlertDialog(onDismissRequest = { model.scope(false) },
            text = { Text(stringResource(R.string.sandbox_scope)) },
            confirmButton = { TextButton({ model.scope(false) }) { Text(stringResource(R.string.sandbox_close)) } })
    }
}

@Composable private fun SandboxScreenContent(scenario: SandboxScenario, enabled: Boolean,
    onScenario: (SandboxScenario) -> Unit, onReset: (Boolean) -> Unit, onTheme: () -> Unit,
    content: @Composable () -> Unit) {
    Surface { Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        SandboxControls(scenario, enabled, onScenario, onReset, onTheme)
        Box(Modifier.weight(1f)) { content() }
    } }
}
@PreviewLightDark @Composable private fun SandboxScreenPreview() = CoffeePeekTheme {
    SandboxScreenContent(SandboxScenario.Ready, true, {}, {}, {}) { Text("Sandbox") }
}
