package com.coffeepeek.admin.feature.notifications.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.notifications.api.NotificationFeature
import org.koin.compose.koinInject

@Composable
internal fun NotificationPreference(label: String, description: String, feature: NotificationFeature = koinInject()) {
    val state by feature.state.collectAsState()
    LaunchedEffect(feature, state.generation) { feature.refresh() }
    NotificationPushSettings(state, feature::setPushEnabled, feature::refresh,
        { Navigator.navigate(Navigator.Screen.Auth) }, label, description)
}
