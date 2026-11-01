package com.coffeepeek.feature.notifications.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.feature.notifications.api.NotificationFeature

@Composable
internal fun NotificationPreference(feature: NotificationFeature, onSignIn: () -> Unit, label: String, description: String) {
    val state by feature.state.collectAsStateWithLifecycle()
    LaunchedEffect(feature, state.generation) { feature.refresh() }
    NotificationPushSettings(state, feature::setPushEnabled, feature::refresh, onSignIn, label, description)
}
