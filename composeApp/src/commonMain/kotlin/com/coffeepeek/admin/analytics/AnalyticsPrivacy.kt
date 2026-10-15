package com.coffeepeek.admin.analytics

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
internal fun AnalyticsPrivacy() {
    val service = Analytics.service ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> service.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
        lifecycle.addObserver(observer)
        service.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose { lifecycle.removeObserver(observer) }
    }
}

@Composable
internal fun AnalyticsConsentSwitch() {
    val service = Analytics.service ?: return
    val state by service.state.collectAsState()
    val scope = rememberCoroutineScope()
    Switch(checked = state.consent == AnalyticsConsent.Accepted,
        onCheckedChange = { checked -> scope.launch { service.setConsent(if (checked) AnalyticsConsent.Accepted else AnalyticsConsent.Declined) } })
}
