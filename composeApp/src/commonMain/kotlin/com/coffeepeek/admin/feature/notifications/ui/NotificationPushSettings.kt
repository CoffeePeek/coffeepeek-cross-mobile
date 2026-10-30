package com.coffeepeek.admin.feature.notifications.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitch
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun NotificationPushSettings(state: NotificationInboxState, onPushChanged: (Boolean) -> Unit, onRefresh: () -> Unit) {
    val canToggle = state.settingsLoaded && state.deviceAvailable && !state.savingSettings
    val checked = state.pushEnabled && state.permissionGranted
    Surface(shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
        Column(Modifier.padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            Row(Modifier.fillMaxWidth().toggleable(value = checked, enabled = canToggle,
                role = Role.Switch, onValueChange = onPushChanged), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                    Text(stringResource(Res.string.notifications_push_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(when {
                        !state.deviceAvailable -> Res.string.notifications_push_unavailable
                        state.savingSettings -> Res.string.notifications_saving
                        !state.permissionGranted -> Res.string.notifications_push_permission
                        else -> Res.string.notifications_push_description
                    }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                CoffeePeekSwitch(checked, null, enabled = canToggle, modifier = Modifier.clearAndSetSemantics { })
            }
            state.settingsError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (!state.settingsLoaded) TextButton(onClick = onRefresh,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text(stringResource(Res.string.notifications_retry_settings))
                }
            }
        }
    }
}

@Composable
private fun NotificationPushSettingsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    NotificationPushSettings(NotificationInboxState(settingsLoaded = true, deviceAvailable = true,
        permissionGranted = true), {}, {})
}

@Preview(widthDp = 360) @Composable private fun NotificationPushSettingsLightPreview() = NotificationPushSettingsPreview(false)
@Preview(widthDp = 360) @Composable private fun NotificationPushSettingsDarkPreview() = NotificationPushSettingsPreview(true)
