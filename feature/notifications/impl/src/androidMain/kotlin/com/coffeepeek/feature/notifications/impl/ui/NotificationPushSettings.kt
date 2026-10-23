package com.coffeepeek.feature.notifications.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.coffeepeek.feature.notifications.impl.R
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitch
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun NotificationPushSettings(state: NotificationInboxState, onPushChanged: (Boolean) -> Unit, onRefresh: () -> Unit) {
    val canToggle = state.settingsLoaded && state.deviceAvailable && !state.savingSettings
    val checked = state.pushEnabled && state.permissionGranted
    val title = stringResource(R.string.notifications_push_title)
    Surface(shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
        Column(Modifier.padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            // Only the switch takes the tap; the text around it is not a button.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(when {
                        !state.deviceAvailable -> R.string.notifications_push_unavailable
                        state.savingSettings -> R.string.notifications_saving
                        !state.permissionGranted -> R.string.notifications_push_permission
                        else -> R.string.notifications_push_description
                    }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                CoffeePeekSwitch(checked, onPushChanged, enabled = canToggle, modifier = Modifier.semantics { contentDescription = title })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(stringResource(R.string.notifications_history_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.settingsError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (!state.settingsLoaded) TextButton(onClick = onRefresh,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text(stringResource(R.string.notifications_retry_settings))
                }
            }
        }
    }
}

@Composable
private fun NotificationPushSettingsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    NotificationPushSettings(NotificationInboxState(settingsLoaded = true, deviceAvailable = true), {}, {})
}

@Preview(widthDp = 360) @Composable private fun NotificationPushSettingsLightPreview() = NotificationPushSettingsPreview(false)
@Preview(widthDp = 360) @Composable private fun NotificationPushSettingsDarkPreview() = NotificationPushSettingsPreview(true)
