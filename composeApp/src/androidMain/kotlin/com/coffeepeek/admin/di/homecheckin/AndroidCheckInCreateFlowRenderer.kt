package com.coffeepeek.admin.di.homecheckin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_create_close
import com.coffeepeek.admin.analytics.AnalyticsOverlay
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistorySession
import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateContent
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateFlowRenderer
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import org.jetbrains.compose.resources.stringResource

/** A new flow lifetime per opening. Never constructs legacy Home state or a second writer. */
internal class AndroidCheckInCreateFlowRenderer(
    private val sessions: SessionRepository,
    private val createEditor: (AndroidCheckInHistorySession, CheckInTracker?) -> HomeCheckInEditorEntry,
) : CheckInCreateFlowRenderer {
    @Composable
    override fun Content(onDismiss: () -> Unit, onShop: () -> Unit, onHomeSaved: () -> Unit,
        legacyContent: @Composable () -> Unit) {
        val owner = viewModel<RetainedViewModelStore>(key = "global-check-in-flow")
        val activity = LocalContext.current.findActivity()
        DisposableEffect(owner) {
            // A rotation or theme change keeps the draft; closing the sheet releases it.
            onDispose { if (activity?.isChangingConfigurations != true) owner.release() }
        }
        val model = viewModel(viewModelStoreOwner = owner) { AndroidGlobalCheckInFlow(sessions, createEditor) }
        val state by model.state.collectAsStateWithLifecycle()
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
        val destination = state
        if (destination is GlobalCheckInDestination.Closed || !model.canAccess()) return
        val home = destination as? GlobalCheckInDestination.Home
        val dismissAllowed = home?.dismissAllowed?.collectAsStateWithLifecycle()?.value ?: true
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
            AnalyticsOverlay {
                CoffeePeekSwipeDismissModalBottomSheet({ model.cancel(destination) }, stringResource(Res.string.check_in_create_close),
                    dismissEnabled = dismissAllowed, maxSheetHeightFraction = if (home != null) 0.9f else null) {
                    if (home == null) CheckInCreateContent(model::shop, model::home)
                    else home.entry.Content(null, 0, model.canAccess(), { model.cancel(home) },
                        { id, _ -> model.saved(home, id) }, { model.setDismissAllowed(home, it) },
                        Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
