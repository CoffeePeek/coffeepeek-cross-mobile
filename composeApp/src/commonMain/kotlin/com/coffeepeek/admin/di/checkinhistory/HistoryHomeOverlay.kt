package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.history_home_close
import coffeepeek.composeapp.generated.resources.history_home_visibility_unconfirmed
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.admin.di.homecheckin.appHomeCheckInDates
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import org.jetbrains.compose.resources.stringResource

/** One app-owned sheet; feature entries render its body without nesting another editor/detail modal. */
@Composable
internal fun HistoryHomeOverlay(bridge: HistoryHomeBridge, session: HistoryHostSession,
    onPhoto: (List<String>, List<String>, Int) -> Unit) {
    // UIKit can stop the presenting screen while its child sheet still needs replacement/dismissal updates.
    val destination by bridge.destination.collectAsState()
    destination?.let { current -> key(current) {
        val dismissAllowed by current.dismissAllowed.collectAsState()
        val dates = remember { appHomeCheckInDates() }
        val allowed = session.lease.isLoggedIn && session.lease.isCurrent()
        AnalyticsOverlay {
            CoffeePeekSwipeDismissModalBottomSheet({ bridge.dismiss(current) }, stringResource(Res.string.history_home_close),
                dismissEnabled = dismissAllowed, maxSheetHeightFraction = 0.9f) {
                // UIKit sheets compose in a separate root: bind the destination store inside the body.
                CompositionLocalProvider(LocalViewModelStoreOwner provides current) {
                    val modifier = Modifier.weight(1f).fillMaxWidth()
                    if (current.editing) current.entries.editor.Content(current.id, session.generation, allowed,
                        { bridge.dismiss(current) }, { id, notice -> bridge.saved(current, id, notice) },
                        { bridge.setDismissAllowed(current, it) }, modifier)
                    else current.entries.detail.Content(requireNotNull(current.id), session.generation, allowed,
                        { civil, instant -> listOf(civil.split('-').reversed().joinToString("."),
                            utcIsoToLocalDateTime(instant).substringAfterLast(' ', "")).filter(String::isNotBlank).joinToString(" · ") }, dates.formatMethod,
                        { bridge.dismiss(current) }, { bridge.edit(current, it) }, { bridge.deleted(current, it) },
                        { urls, originals, index -> bridge.openPhoto(current, urls, originals, index, onPhoto) },
                        { bridge.setDismissAllowed(current, it) }, modifier,
                        if (current.visibilityUnconfirmed) stringResource(Res.string.history_home_visibility_unconfirmed) else null)
                }
            }
        }
    } }
}
