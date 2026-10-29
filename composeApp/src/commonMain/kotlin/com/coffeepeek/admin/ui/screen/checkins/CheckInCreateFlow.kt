package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.analytics.CheckInKind
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff
import com.coffeepeek.admin.feature.homecheckin.ui.HomeCheckInViewModel
import com.coffeepeek.admin.feature.homecheckin.ui.HomeEditor
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.admin.ui.screen.shelf.ShelfScreenRenderer
import org.koin.compose.koinInject

/** Platform-selected global flow. Resolve legacy form state only inside the shared/iOS fallback, which offers the shelf where the build has it. */
@Composable
internal fun CheckInCreateFlow(
    onDismiss: () -> Unit,
    onShop: () -> Unit,
    onHomeSaved: () -> Unit = {},
    homeVm: HomeCheckInViewModel? = null,
    renderer: CheckInCreateFlowRenderer = koinInject(),
) {
    renderer.Content(onDismiss, onShop, onHomeSaved) {
        val shelf = koinInject<ShelfScreenRenderer>().available
        LegacyCheckInCreateFlow(onDismiss, onShop, onHomeSaved, homeVm ?: platformViewModel(), shelf)
    }
}

@Composable
private fun LegacyCheckInCreateFlow(
    onDismiss: () -> Unit,
    onShop: () -> Unit,
    onHomeSaved: () -> Unit,
    homeVm: HomeCheckInViewModel,
    shelf: Boolean,
) {
    val tracker = remember(homeVm) { homeVm.creationTracker() }
    val home by homeVm.state.collectAsState()
    var creatingHome by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(creatingHome, home.editing) {
        if (creatingHome && !home.editing) {
            if (home.selected != null) onHomeSaved()
            homeVm.creationPickerFinished()
            onDismiss()
        }
    }
    SwipeDismissModalBottomSheet(
        onDismissRequest = { tracker.cancel("close"); homeVm.creationPickerFinished(); if (creatingHome) homeVm.closeEditor(); onDismiss() },
        dismissEnabled = !home.saving,
    ) {
        if (creatingHome) HomeEditor(home, homeVm, Modifier.fillMaxWidth().heightIn(max = 680.dp))
        else CheckInCreateContent(onShop = { tracker.kind(CheckInKind.Visit); CheckInFlowHandoff.tracker = tracker; homeVm.creationPickerFinished(); onShop() },
            onHome = { tracker.kind(CheckInKind.Home); homeVm.create(tracker); creatingHome = true },
            // As in the migrated flow: the picker closes and the shelf's scan takes over; no check-in is started.
            onShelfBag = { homeVm.creationPickerFinished(); onDismiss(); Navigator.navigate(Navigator.Screen.ShelfAddBag) }.takeIf { shelf })
    }
}
