package com.coffeepeek.admin.di.homecheckin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_create_close
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistorySession
import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateContent
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import org.jetbrains.compose.resources.stringResource

/**
 * A new flow lifetime per opening. Never constructs legacy Home state or a second writer. With [shelfEnabled] the picker also
 * offers "Пачку на полку", which hands over to the shelf's scan.
 */
internal class AndroidCheckInCreateFlowRenderer(
    private val sessions: SessionReader,
    private val shelfEnabled: Boolean,
    private val createEditor: (AndroidCheckInHistorySession, CheckInTracker?) -> HomeCheckInEditorEntry,
) : CheckInCreateFlowRenderer {
    @Composable
    override fun Content(onDismiss: () -> Unit, onShop: () -> Unit, onHomeSaved: () -> Unit,
        legacyContent: @Composable () -> Unit) {
        val owner = viewModel(key = "global-check-in-flow-store") { AndroidGlobalCheckInFlowStore() }
        val activity = LocalActivity.current
        DisposableEffect(owner, activity) { onDispose { owner.dispose(activity?.isChangingConfigurations == true) } }
        val model = viewModel(viewModelStoreOwner = owner) { AndroidGlobalCheckInFlow(sessions, createEditor) }
        val state by model.state.collectAsStateWithLifecycle()
        Completion(model, onDismiss, onShop, onHomeSaved)
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { Destination(model, state) }
    }

    @Composable
    private fun Completion(model: AndroidGlobalCheckInFlow, onDismiss: () -> Unit, onShop: () -> Unit, onHomeSaved: () -> Unit) {
        val currentDismiss by rememberUpdatedState(onDismiss)
        val currentShop by rememberUpdatedState(onShop)
        val currentSaved by rememberUpdatedState(onHomeSaved)
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(model, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { destination ->
                    if (destination is GlobalCheckInDestination.Closed) model.claim(destination)?.let { completion ->
                        when (completion.outcome) {
                            GlobalCheckInOutcome.Close -> currentDismiss()
                            GlobalCheckInOutcome.Saved -> try { currentSaved() } finally { currentDismiss() }
                            GlobalCheckInOutcome.Shop -> {
                                CheckInFlowHandoff.tracker = completion.shopTracker
                                currentShop()
                            }
                            GlobalCheckInOutcome.ShelfBag -> try { Navigator.navigate(Navigator.Screen.ShelfAddBag) } finally { currentDismiss() }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Destination(model: AndroidGlobalCheckInFlow, state: GlobalCheckInDestination) {
        val destination = state
        if (destination is GlobalCheckInDestination.Closed || !model.canAccess()) return
        val home = destination as? GlobalCheckInDestination.Home
        val dismissAllowed = home?.dismissAllowed?.collectAsStateWithLifecycle()?.value ?: true
        AnalyticsOverlay {
            CoffeePeekSwipeDismissModalBottomSheet({ model.cancel(destination) }, stringResource(Res.string.check_in_create_close),
                dismissEnabled = dismissAllowed, maxSheetHeightFraction = if (home != null) 0.9f else null) {
                if (home == null) CheckInCreateContent(model::shop, model::home, (model::shelfBag).takeIf { shelfEnabled })
                else home.entry.Content(null, 0, model.canAccess(), { model.cancel(home) },
                    { id, _ -> model.saved(home, id) }, { model.setDismissAllowed(home, it) },
                    Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}
