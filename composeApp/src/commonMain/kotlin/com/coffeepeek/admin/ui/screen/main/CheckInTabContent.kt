package com.coffeepeek.admin.ui.screen.main

import com.coffeepeek.admin.analytics.*
import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateButton
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlow
import com.coffeepeek.core.session.SessionReader
import org.koin.compose.koinInject

/** Wires tab content to the shared check-in flow in both platform navigation hosts. */
@Composable
internal fun CheckInTabContent(
    showCreateButton: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val sessions: SessionReader = koinInject()
    val session by remember(sessions) { sessions.observeSession() }.collectAsState(sessions.peekSession())
    var showCreate by androidx.compose.runtime.saveable.rememberSaveable(session?.userId ?: session?.accessToken) { mutableStateOf(false) }
    val clearance = LocalFloatingNavClearance.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Box(Modifier.fillMaxSize()) {
        content()
        if (showCreateButton) {
            CheckInCreateButton(
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = CpDimens.spacing4, bottom = clearance + 12.dp),
            ) {
                focusManager.clearFocus(force = true)
                keyboard?.hide()
                Analytics.track(AnalyticsEvent.CHECK_IN_START_ATTEMPTED, "source" to "global_create")
                if (sessions.isActiveSession(sessions.peekSession())) showCreate = true
                else { Analytics.gate("check_in"); Navigator.navigate(Navigator.Screen.Auth) }
            }
        }
    }

    if (showCreate) CheckInCreateFlow(
        onDismiss = { showCreate = false },
        onShop = { showCreate = false; Navigator.navigate(Navigator.Screen.CreateCheckIn) },
    )
}
