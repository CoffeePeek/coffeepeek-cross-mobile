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
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import org.koin.compose.koinInject

/** Platform-selected global flow. Resolve legacy form state only inside the explicit fallback. */
@Composable
internal fun CheckInCreateFlow(
    onDismiss: () -> Unit,
    onShop: () -> Unit,
    onHomeSaved: () -> Unit = {},
    homeVm: HomeCheckInViewModel? = null,
    renderer: CheckInCreateFlowRenderer = koinInject(),
) {
    renderer.Content(onDismiss, onShop, onHomeSaved) {
        LegacyCheckInCreateFlow(onDismiss, onShop, onHomeSaved, homeVm ?: platformViewModel())
    }
}

@Composable
private fun LegacyCheckInCreateFlow(
    onDismiss: () -> Unit,
    onShop: () -> Unit,
    onHomeSaved: () -> Unit,
    homeVm: HomeCheckInViewModel,
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
            onHome = { tracker.kind(CheckInKind.Home); homeVm.create(tracker); creatingHome = true })
    }
}
