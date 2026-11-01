package com.coffeepeek.admin.feature.notifications.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import com.coffeepeek.feature.notifications.api.NotificationSettingsError
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
internal fun NotificationSettingsScreen(onBack: () -> Unit, feature: NotificationFeature = koinInject()) {
    val state by feature.state.collectAsState()
    LaunchedEffect(feature, state.generation) { feature.refresh() }
    NotificationSettingsContent(state, feature::setPushEnabled, feature::refresh,
        { Navigator.navigate(Navigator.Screen.Auth) }, onBack)
}

@Composable
internal fun NotificationSettingsContent(
    state: NotificationInboxState, onPushChanged: (Boolean) -> Unit, onRefresh: () -> Unit,
    onSignIn: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier,
) {
    CoffeePeekScaffold(modifier, topBar = { CpTopBar(stringResource(Res.string.notifications_title), onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(coffeePeekScrollContentPadding(padding))
            .verticalScroll(rememberScrollState()).padding(horizontal = CoffeePeekDimens.spacing5, vertical = CoffeePeekDimens.spacing4)) {
            if (state.accountId == null) {
                NotificationEmptyState(signedOut = true, onSignIn = onSignIn,
                    description = stringResource(Res.string.notifications_settings_sign_in_description))
            } else {
                NotificationPushSettings(state, onPushChanged, onRefresh)
            }
        }
    }
}

@Composable
private fun NotificationSettingsPreview(dark: Boolean, signedIn: Boolean = true, error: Boolean = false, compact: Boolean = false) =
    CoffeePeekTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, if (compact) 2f else 1f)) {
            val state = NotificationInboxState(accountId = if (signedIn) "preview" else null,
                settingsLoaded = !error, deviceAvailable = true, permissionGranted = true,
                settingsError = if (error) NotificationSettingsError.LoadFailed else null)
            NotificationSettingsContent(state, {}, {}, {}, {}, Modifier.size(360.dp, if (compact) 320.dp else 640.dp))
        }
    }

@Preview @Composable private fun NotificationSettingsLightPreview() = NotificationSettingsPreview(false)
@Preview @Composable private fun NotificationSettingsDarkPreview() = NotificationSettingsPreview(true)
@Preview @Composable private fun NotificationSettingsGuestLightPreview() = NotificationSettingsPreview(false, signedIn = false)
@Preview @Composable private fun NotificationSettingsGuestDarkPreview() = NotificationSettingsPreview(true, signedIn = false)
@Preview @Composable private fun NotificationSettingsErrorLightPreview() = NotificationSettingsPreview(false, error = true)
@Preview @Composable private fun NotificationSettingsErrorDarkPreview() = NotificationSettingsPreview(true, error = true)
@Preview @Composable private fun NotificationSettingsCompactLightPreview() = NotificationSettingsPreview(false, compact = true)
@Preview @Composable private fun NotificationSettingsCompactDarkPreview() = NotificationSettingsPreview(true, compact = true)
