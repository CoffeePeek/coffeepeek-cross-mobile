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
import com.coffeepeek.admin.sandbox.backend.SandboxAuthPolicy
import com.coffeepeek.admin.sandbox.backend.SandboxCities
import com.coffeepeek.admin.sandbox.backend.SandboxFeed
import com.coffeepeek.admin.sandbox.backend.SandboxScenario
import com.coffeepeek.admin.sandbox.backend.SandboxUpdatePolicy
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable internal fun SandboxControls(scenario: SandboxScenario, enabled: Boolean,
    onScenario: (SandboxScenario) -> Unit, onReset: (Boolean) -> Unit, onTheme: () -> Unit,
    updatePolicy: SandboxUpdatePolicy = SandboxUpdatePolicy.UpToDate, onUpdatePolicy: (SandboxUpdatePolicy) -> Unit = {},
    cities: SandboxCities = SandboxCities.Single, onCities: (SandboxCities) -> Unit = {},
    authPolicy: SandboxAuthPolicy = SandboxAuthPolicy.Works, onAuthPolicy: (SandboxAuthPolicy) -> Unit = {},
    feed: SandboxFeed = SandboxFeed.Single, onFeed: (SandboxFeed) -> Unit = {}) {
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
            Text(stringResource(R.string.sandbox_update_title), style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SandboxUpdatePolicy.entries.forEach { mode ->
                    val label = when (mode) {
                        SandboxUpdatePolicy.UpToDate -> R.string.sandbox_update_current
                        SandboxUpdatePolicy.Available -> R.string.sandbox_update_available
                        SandboxUpdatePolicy.Required -> R.string.sandbox_update_required
                        SandboxUpdatePolicy.Failure -> R.string.sandbox_update_failure
                    }
                    FilterChip(updatePolicy == mode, { onUpdatePolicy(mode) }, { Text(stringResource(label)) }, enabled = enabled)
                }
            }
            Text(stringResource(R.string.sandbox_cities_title), style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SandboxCities.entries.forEach { mode ->
                    val label = when (mode) {
                        SandboxCities.Single -> R.string.sandbox_cities_single
                        SandboxCities.Several -> R.string.sandbox_cities_several
                        SandboxCities.Empty -> R.string.sandbox_cities_empty
                        SandboxCities.Failure -> R.string.sandbox_cities_failure
                    }
                    FilterChip(cities == mode, { onCities(mode) }, { Text(stringResource(label)) }, enabled = enabled)
                }
            }
            Text(stringResource(R.string.sandbox_auth_title), style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SandboxAuthPolicy.entries.forEach { mode ->
                    val label = when (mode) {
                        SandboxAuthPolicy.Works -> R.string.sandbox_auth_works
                        SandboxAuthPolicy.WrongPassword -> R.string.sandbox_auth_wrong_password
                        SandboxAuthPolicy.EmailTaken -> R.string.sandbox_auth_email_taken
                        SandboxAuthPolicy.RegistrationFailure -> R.string.sandbox_auth_registration_failure
                        SandboxAuthPolicy.RecoveryFailure -> R.string.sandbox_auth_recovery_failure
                    }
                    FilterChip(authPolicy == mode, { onAuthPolicy(mode) }, { Text(stringResource(label)) }, enabled = enabled)
                }
            }
            Text(stringResource(R.string.sandbox_feed_title), style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SandboxFeed.entries.forEach { mode ->
                    val label = when (mode) {
                        SandboxFeed.Single -> R.string.sandbox_feed_single
                        SandboxFeed.Paged -> R.string.sandbox_feed_paged
                        SandboxFeed.PageFailsOnce -> R.string.sandbox_feed_page_fails_once
                        SandboxFeed.InvalidCursor -> R.string.sandbox_feed_invalid_cursor
                        SandboxFeed.Empty -> R.string.sandbox_feed_empty
                        SandboxFeed.Failure -> R.string.sandbox_feed_failure
                    }
                    FilterChip(feed == mode, { onFeed(mode) }, { Text(stringResource(label)) }, enabled = enabled)
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
