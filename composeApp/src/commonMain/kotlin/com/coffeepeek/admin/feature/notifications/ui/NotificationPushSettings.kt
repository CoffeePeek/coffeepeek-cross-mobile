package com.coffeepeek.admin.feature.notifications.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitch
import com.coffeepeek.core.designsystem.component.CoffeePeekSettingsRow
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import com.coffeepeek.feature.notifications.api.NotificationSettingsError
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun NotificationPushSettings(
    state: NotificationInboxState, onPushChanged: (Boolean) -> Unit, onRefresh: () -> Unit, onSignIn: () -> Unit,
    label: String, description: String,
) {
    val guest = state.accountId == null
    val checked = state.pushPreferenceChecked
    Column {
        CoffeePeekSettingsRow(CoffeePeekIcons.Notifications, label, notificationPreferenceDescription(state, description),
            onClick = { if (guest) onSignIn() else onPushChanged(!checked) },
            enabled = guest || state.canChangePushPreference, iconColors = CoffeePeekIconBadgePalette.Gold,
            role = if (guest) Role.Button else Role.Switch,
            modifier = if (guest) Modifier else Modifier.semantics { toggleableState = ToggleableState(checked) },
            trailing = { CoffeePeekSwitch(checked, null, Modifier.clearAndSetSemantics { }, enabled = state.canChangePushPreference) })
        if (!guest) NotificationPreferenceFeedback(state, onRefresh)
    }
}

@Composable
private fun notificationPreferenceDescription(state: NotificationInboxState, description: String): String = when {
    state.accountId == null -> stringResource(Res.string.notifications_settings_sign_in_description)
    !state.deviceAvailable -> stringResource(Res.string.notifications_push_unavailable)
    state.savingSettings -> stringResource(Res.string.notifications_saving)
    !state.settingsLoaded -> stringResource(Res.string.notifications_loading)
    !state.permissionGranted -> stringResource(Res.string.notifications_push_permission)
    else -> description
}

@Composable
private fun NotificationPreferenceFeedback(state: NotificationInboxState, onRefresh: () -> Unit) {
    val error = state.settingsError ?: return
    Column(Modifier.padding(horizontal = CoffeePeekDimens.settingsRowPaddingH, vertical = CoffeePeekDimens.spacing2)) {
        Text(stringResource(when (error) {
            NotificationSettingsError.LoadFailed -> Res.string.legacy_notifications_push_load_failed
            NotificationSettingsError.SaveFailed -> Res.string.legacy_notifications_push_save_failed
        }), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        if (!state.settingsLoaded) TextButton(onClick = onRefresh,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
            Text(stringResource(Res.string.notifications_retry_settings))
        }
    }
}

@Composable
private fun NotificationPushSettingsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
        val states = listOf(NotificationInboxState(),
            NotificationInboxState(accountId = "preview", settingsLoaded = true, deviceAvailable = true, permissionGranted = true),
            NotificationInboxState(accountId = "preview", deviceAvailable = true, settingsError = NotificationSettingsError.LoadFailed),
            NotificationInboxState(accountId = "preview", settingsLoaded = true, deviceAvailable = true,
                settingsError = NotificationSettingsError.SaveFailed))
        states.forEach { state ->
            NotificationPushSettings(state, {}, {}, {}, stringResource(Res.string.notifications_title), stringResource(Res.string.notifications_push_description))
        }
    }
}

@Preview(widthDp = 360) @Composable private fun NotificationPushSettingsLightPreview() = NotificationPushSettingsPreview(false)
@Preview(widthDp = 360) @Composable private fun NotificationPushSettingsDarkPreview() = NotificationPushSettingsPreview(true)

@Composable
private fun NotificationPreferenceLargeTextPreview(dark: Boolean) {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
        NotificationPushSettingsPreview(dark)
    }
}

@Preview(widthDp = 320) @Composable private fun NotificationPreferenceLargeTextLightPreview() = NotificationPreferenceLargeTextPreview(false)
@Preview(widthDp = 320) @Composable private fun NotificationPreferenceLargeTextDarkPreview() = NotificationPreferenceLargeTextPreview(true)
