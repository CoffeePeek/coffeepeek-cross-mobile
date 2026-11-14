package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.history_home_close
import coffeepeek.composeapp.generated.resources.history_home_visibility_unconfirmed
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.admin.di.homecheckin.appHomeCheckInDates
import com.coffeepeek.admin.di.homecheckin.HomeCheckInFullScreenDialog
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

/** Application-owned presentation: full-screen editor and detail sheet with one destination lifetime. */
@Composable
internal fun HistoryHomeOverlay(bridge: HistoryHomeBridge, session: HistoryHostSession,
    onPhoto: (List<String>, List<String>, Int) -> Unit) {
    val destination by bridge.destination.collectAsStateWithLifecycle()
    destination?.let { current -> key(current) {
        val dismissAllowed by current.dismissAllowed.collectAsStateWithLifecycle()
        val dates = remember { appHomeCheckInDates() }
        val allowed = session.lease.isLoggedIn && session.lease.isCurrent()
        AnalyticsOverlay {
            if (current.editing) HomeCheckInFullScreenDialog({ if (dismissAllowed) bridge.dismiss(current) }) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides current) {
                    current.entries.editor.Content(current.id, session.generation, allowed,
                        { bridge.dismiss(current) }, { id, notice -> bridge.saved(current, id, notice) },
                        { bridge.setDismissAllowed(current, it) }, Modifier.fillMaxSize(), bean = null)
                }
            } else CoffeePeekSwipeDismissModalBottomSheet({ bridge.dismiss(current) },
                stringResource(Res.string.history_home_close), dismissEnabled = dismissAllowed,
                maxSheetHeightFraction = 0.9f) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides current) {
                    current.entries.detail.Content(requireNotNull(current.id), session.generation, allowed,
                        { civil, instant -> listOf(civil.split('-').reversed().joinToString("."),
                            utcIsoToLocalDateTime(instant).substringAfterLast(' ', "")).filter(String::isNotBlank).joinToString(" · ") }, dates.formatMethod,
                        { bridge.dismiss(current) }, { bridge.edit(current, it) }, { bridge.deleted(current, it) },
                        { urls, originals, index -> bridge.openPhoto(current, urls, originals, index, onPhoto) },
                        { bridge.setDismissAllowed(current, it) }, Modifier.weight(1f).fillMaxWidth(),
                        if (current.visibilityUnconfirmed) stringResource(Res.string.history_home_visibility_unconfirmed) else null)
                }
            }
        }
    } }
}
