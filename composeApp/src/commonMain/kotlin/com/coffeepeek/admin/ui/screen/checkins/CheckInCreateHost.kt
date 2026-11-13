package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.ui.Navigator

/**
 * The one global create action: a guest is sent to sign in, a signed-in user gets the creation flow.
 * [resetKey] closes an open flow when the account scope changes.
 */
@Composable
internal fun CheckInCreateHost(
    canCreate: () -> Boolean,
    resetKey: Any?,
    onShop: () -> Unit,
    onHomeSaved: () -> Unit = {},
    content: @Composable (onCreate: () -> Unit) -> Unit,
) {
    var showCreate by rememberSaveable(resetKey) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val onCreate = {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        Analytics.track(AnalyticsEvent.CHECK_IN_START_ATTEMPTED, "source" to "global_create")
        if (canCreate()) showCreate = true
        else { Analytics.gate("check_in"); Navigator.navigate(Navigator.Screen.Auth) }
    }
    content(onCreate)
    if (showCreate) CheckInCreateFlow(
        onDismiss = { showCreate = false },
        onShop = { showCreate = false; onShop() },
        onHomeSaved = onHomeSaved,
    )
}
