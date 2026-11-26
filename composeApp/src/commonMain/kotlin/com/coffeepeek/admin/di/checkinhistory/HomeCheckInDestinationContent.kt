package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.history_home_close
import coffeepeek.composeapp.generated.resources.history_home_visibility_unconfirmed
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.admin.di.homecheckin.appHomeCheckInDates
import com.coffeepeek.admin.di.homecheckin.HomeCheckInFullScreenDialog
import com.coffeepeek.admin.ui.component.NativeNavigationBar
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.core.designsystem.component.CoffeePeekCircularBackButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

/** Detail occupies its own route; editing retains the existing full-screen modal and draft lifetime. */
@Composable
internal fun HomeCheckInDestinationContent(bridge: HistoryHomeBridge, session: HistoryHostSession,
    onPhoto: (List<String>, List<String>, Int) -> Unit) {
    val destination by bridge.destination.collectAsStateWithLifecycle()
    destination?.let { current -> key(current) {
        NativeNavigationBar("", { bridge.dismiss(current) }, customBack = true, action = null)
        if (current.editing) HomeCheckInEditContent(current, bridge, session)
        else HomeCheckInDetailContent(current, bridge, session, onPhoto)
    } }
}

@Composable
private fun HomeCheckInEditContent(current: HistoryHomeDestination, bridge: HistoryHomeBridge,
    session: HistoryHostSession) {
    val dismissAllowed by current.dismissAllowed.collectAsStateWithLifecycle()
    AnalyticsOverlay {
        HomeCheckInFullScreenDialog({ if (dismissAllowed) bridge.dismiss(current) }) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides current) {
                current.entries.editor.Content(current.id, session.generation,
                    session.lease.isLoggedIn && session.lease.isCurrent(),
                    { bridge.dismiss(current) }, { id, notice -> bridge.saved(current, id, notice) },
                    { bridge.setDismissAllowed(current, it) }, Modifier.fillMaxSize(), bean = null)
            }
        }
    }
}

@Composable
private fun HomeCheckInDetailContent(current: HistoryHomeDestination, bridge: HistoryHomeBridge,
    session: HistoryHostSession, onPhoto: (List<String>, List<String>, Int) -> Unit) {
    val dates = remember { appHomeCheckInDates() }
    CompositionLocalProvider(LocalViewModelStoreOwner provides current) {
        Column(Modifier.fillMaxSize()) {
            CoffeePeekCircularBackButton(onClick = { bridge.dismiss(current) },
                contentDescription = stringResource(Res.string.history_home_close),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            current.entries.detail.Content(requireNotNull(current.id), session.generation,
                session.lease.isLoggedIn && session.lease.isCurrent(), ::formatHomePreparedAt, dates.formatMethod,
                { bridge.dismiss(current) }, { bridge.edit(current, it) }, { bridge.deleted(current, it) },
                { urls, originals, index -> bridge.openPhoto(current, urls, originals, index, onPhoto) },
                { bridge.setDismissAllowed(current, it) }, Modifier.weight(1f),
                if (current.visibilityUnconfirmed) stringResource(Res.string.history_home_visibility_unconfirmed) else null)
        }
    }
}

private fun formatHomePreparedAt(civil: String, instant: String): String =
    listOf(civil.split('-').reversed().joinToString("."),
        utcIsoToLocalDateTime(instant).substringAfterLast(' ', "")).filter(String::isNotBlank).joinToString(" · ")
