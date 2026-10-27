package com.coffeepeek.admin.ui.screen.main


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateHost
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

    Box(Modifier.fillMaxSize()) {
        content()
        CheckInCreateHost(
            canCreate = { sessions.isActiveSession(sessions.peekSession()) },
            resetKey = session?.userId ?: session?.accessToken,
            onShop = { Navigator.navigate(Navigator.Screen.CreateCheckIn) },
            showButton = showCreateButton,
        )
    }
}
