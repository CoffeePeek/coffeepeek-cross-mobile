package com.coffeepeek.feature.notifications.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationInboxState
import com.coffeepeek.feature.notifications.api.NotificationSettingsError
import com.coffeepeek.feature.notifications.impl.R

@Composable
internal fun NotificationSettingsScreen(feature: NotificationFeature, onSignIn: () -> Unit, onBack: () -> Unit) {
    val state by feature.state.collectAsStateWithLifecycle()
    LaunchedEffect(feature, state.generation) { feature.refresh() }
    NotificationSettingsContent(state, feature::setPushEnabled, feature::refresh, onSignIn, onBack)
}

@Composable
internal fun NotificationSettingsContent(
    state: NotificationInboxState, onPushChanged: (Boolean) -> Unit, onRefresh: () -> Unit,
    onSignIn: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier,
) {
    CoffeePeekScaffold(modifier, topBar = {
        CoffeePeekTopBar(stringResource(R.string.notifications_title), stringResource(R.string.notifications_back), onBack = onBack)
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(coffeePeekScrollContentPadding(padding))
            .verticalScroll(rememberScrollState()).padding(horizontal = CoffeePeekDimens.spacing5, vertical = CoffeePeekDimens.spacing4)) {
            if (state.accountId == null) {
                NotificationEmptyState(signedOut = true, onSignIn = onSignIn,
                    description = stringResource(R.string.notifications_settings_sign_in_description))
            } else {
                NotificationPushSettings(state, onPushChanged, onRefresh)
            }
        }
    }
}

@Composable
private fun NotificationSettingsPreview(signedIn: Boolean = true, error: Boolean = false, compact: Boolean = false) = CoffeePeekTheme {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, if (compact) 2f else 1f)) {
        val state = NotificationInboxState(accountId = if (signedIn) "preview" else null,
            settingsLoaded = !error, deviceAvailable = true, permissionGranted = true,
            settingsError = if (error) NotificationSettingsError.LoadFailed else null)
        NotificationSettingsContent(state, {}, {}, {}, {}, Modifier.size(360.dp, if (compact) 320.dp else 640.dp))
    }
}

@PreviewLightDark @Composable private fun NotificationSettingsSignedInPreview() = NotificationSettingsPreview()
@PreviewLightDark @Composable private fun NotificationSettingsGuestPreview() = NotificationSettingsPreview(signedIn = false)
@PreviewLightDark @Composable private fun NotificationSettingsErrorPreview() = NotificationSettingsPreview(error = true)
@PreviewLightDark @Composable private fun NotificationSettingsCompactPreview() = NotificationSettingsPreview(compact = true)
