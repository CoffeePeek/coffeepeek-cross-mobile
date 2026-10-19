package com.coffeepeek.admin.sandbox.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.R
import com.coffeepeek.admin.sandbox.backend.SandboxScenario
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable internal fun SandboxControls(scenario: SandboxScenario, enabled: Boolean,
    onScenario: (SandboxScenario) -> Unit, onReset: (Boolean) -> Unit, onTheme: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Text(stringResource(R.string.sandbox_banner), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SandboxScenario.entries.forEach { mode ->
                    val label = when (mode) {
                        SandboxScenario.Ready -> R.string.sandbox_ready
                        SandboxScenario.Slow -> R.string.sandbox_slow
                        SandboxScenario.Offline -> R.string.sandbox_offline
                        SandboxScenario.ServerError -> R.string.sandbox_server_error
                        SandboxScenario.SaveError -> R.string.sandbox_save_error
                        SandboxScenario.UploadError -> R.string.sandbox_upload_error
                    }
                    FilterChip(scenario == mode, { onScenario(mode) }, { Text(stringResource(label)) }, enabled = enabled)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton({ onReset(true) }, enabled = enabled) { Text(stringResource(R.string.sandbox_reset)) }
                TextButton({ onReset(false) }, enabled = enabled) { Text(stringResource(R.string.sandbox_empty)) }
                TextButton(onTheme) { Text(stringResource(R.string.sandbox_theme)) }
            }
        }
    }
}
@PreviewLightDark @Composable private fun SandboxControlsPreview() = CoffeePeekTheme {
    SandboxControls(SandboxScenario.Ready, true, {}, {}, {})
}
