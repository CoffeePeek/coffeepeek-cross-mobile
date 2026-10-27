package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance

/**
 * The one global create action: a guest is sent to sign in, a signed-in user gets the creation flow.
 * [resetKey] closes an open flow when the account scope changes. Position is explicit: Scaffold's own FAB inset would add another 16 dp.
 */
@Composable
internal fun BoxScope.CheckInCreateHost(
    canCreate: () -> Boolean,
    resetKey: Any?,
    onShop: () -> Unit,
    onHomeSaved: () -> Unit = {},
    showButton: Boolean = true,
) {
    var showCreate by rememberSaveable(resetKey) { mutableStateOf(false) }
    val clearance = LocalFloatingNavClearance.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    if (showButton) {
        CheckInCreateButton(Modifier.align(Alignment.BottomEnd).padding(end = CpDimens.spacing4, bottom = clearance + 12.dp)) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            Analytics.track(AnalyticsEvent.CHECK_IN_START_ATTEMPTED, "source" to "global_create")
            if (canCreate()) showCreate = true
            else { Analytics.gate("check_in"); Navigator.navigate(Navigator.Screen.Auth) }
        }
    }
    if (showCreate) CheckInCreateFlow(
        onDismiss = { showCreate = false },
        onShop = { showCreate = false; onShop() },
        onHomeSaved = onHomeSaved,
    )
}
