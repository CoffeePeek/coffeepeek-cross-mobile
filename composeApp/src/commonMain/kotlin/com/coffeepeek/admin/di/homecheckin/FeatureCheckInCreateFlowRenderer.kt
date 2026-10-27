package com.coffeepeek.admin.di.homecheckin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_create_close
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff
import com.coffeepeek.feature.homecheckin.api.CheckInCreatePickerEntry
import com.coffeepeek.feature.homecheckin.impl.createCheckInCreatePickerEntry
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import org.jetbrains.compose.resources.stringResource

/** A new flow lifetime per opening. Never constructs legacy Home state or a second writer. */
internal class FeatureCheckInCreateFlowRenderer(
    private val sessions: SessionReader,
    private val picker: CheckInCreatePickerEntry = createCheckInCreatePickerEntry(),
    private val onCreated: () -> Unit = {},
    private val createEditor: (CheckInHistorySession, CheckInTracker?) -> HomeCheckInEditorEntry,
) : CheckInCreateFlowRenderer {
    @Composable
    override fun Content(onDismiss: () -> Unit, onShop: () -> Unit, onHomeSaved: () -> Unit) {
        val owner = viewModel(key = "global-check-in-flow-store") { GlobalCheckInFlowStore() }
        GlobalCheckInFlowLifetimeEffect(owner)
        val model = viewModel(viewModelStoreOwner = owner) { GlobalCheckInFlow(sessions, onCreated, createEditor) }
        val state by model.state.collectAsState()
        Completion(model, onDismiss, onShop, onHomeSaved)
        Destination(owner, model, state)
    }

    @Composable
    private fun Completion(model: GlobalCheckInFlow, onDismiss: () -> Unit, onShop: () -> Unit, onHomeSaved: () -> Unit) {
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
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Destination(owner: GlobalCheckInFlowStore, model: GlobalCheckInFlow, state: GlobalCheckInDestination) {
        val destination = state
        if (destination is GlobalCheckInDestination.Closed || !model.canAccess()) return
        val home = destination as? GlobalCheckInDestination.Home
        val dismissAllowed = home?.dismissAllowed?.collectAsState()?.value ?: true
        AnalyticsOverlay {
            CoffeePeekSwipeDismissModalBottomSheet({ model.cancel(destination) }, stringResource(Res.string.check_in_create_close),
                dismissEnabled = dismissAllowed, maxSheetHeightFraction = if (home != null) 0.9f else null) {
                // UIKit sheets have their own Compose root; bind the flow store inside their body.
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    if (home == null) Picker(model)
                    else home.entry.Content(null, 0, model.canAccess(), { model.cancel(home) },
                        { id, _ -> model.saved(home, id) }, { model.setDismissAllowed(home, it) },
                        Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
    @Composable
    private fun Picker(model: GlobalCheckInFlow) {
        val page = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
        picker.Content(model::shop, model::home,
            Modifier.analyticsExposure("home_check_in", "check_in_type_picker", page))
    }
}
