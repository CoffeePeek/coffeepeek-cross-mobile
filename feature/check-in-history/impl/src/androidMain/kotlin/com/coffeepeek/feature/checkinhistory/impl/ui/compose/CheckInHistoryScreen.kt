package com.coffeepeek.feature.checkinhistory.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.CheckInHistoryViewModel
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CheckInHistoryScreen(viewModel: CheckInHistoryViewModel, sessionGeneration: Int,
    isLoggedIn: Boolean, contentGeneration: Int, formatCheckInDate: (String) -> String,
    imageContent: HistoryImageContent, onEvent: (CheckInHistoryEvent) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentEvent by rememberUpdatedState(onEvent)
    val generationNow by rememberUpdatedState(sessionGeneration)
    val loggedInNow by rememberUpdatedState(isLoggedIn)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { envelope ->
            if (envelope.isDeliverableTo(generationNow, loggedInNow)) currentEvent(envelope.event)
        }
    }
    LaunchedEffect(viewModel, sessionGeneration, isLoggedIn) {
        viewModel.onAction(CheckInHistoryAction.SessionChanged(sessionGeneration, isLoggedIn))
    }
    LaunchedEffect(viewModel, contentGeneration) { viewModel.onAction(CheckInHistoryAction.ContentChanged(contentGeneration)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(CheckInHistoryAction.Resume) }
    // Hide old private data before the asynchronously processed session action.
    val visible = state.visibleTo(sessionGeneration, isLoggedIn)
    CheckInHistoryScreenContent(visible, viewModel::onAction, formatCheckInDate, imageContent)
}

@Composable
internal fun CheckInHistoryScreenContent(state: CheckInHistoryState, onAction: (CheckInHistoryAction) -> Unit,
    formatCheckInDate: (String) -> String = { it.substringBefore('T') }, imageContent: HistoryImageContent? = null) {
    key(state.sessionGeneration) {
        Scaffold(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            topBar = { CoffeePeekTopBar(stringResource(Res.string.history_title), stringResource(Res.string.history_back),
                onBack = { onAction(CheckInHistoryAction.Back) }) },
            containerColor = MaterialTheme.colorScheme.background) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                val calendarLabel = stringResource(Res.string.history_calendar)
                val listLabel = stringResource(Res.string.history_list)
                CoffeePeekCapsuleSegmentedControl(CheckInHistoryMode.entries, state.mode,
                    { if (it == CheckInHistoryMode.Calendar) calendarLabel else listLabel },
                    { onAction(CheckInHistoryAction.SelectMode(it)) },
                    Modifier.padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2))
                if (!state.isLoggedIn) HistoryGuestPrompt({ onAction(CheckInHistoryAction.SignIn) })
                else when (state.mode) {
                    CheckInHistoryMode.Calendar -> HistoryCalendarContent(state.calendar, state.today, state.canGoNextMonth,
                        onAction, formatCheckInDate, Modifier.weight(1f), imageContent)
                    CheckInHistoryMode.List -> HistoryListContent(state.list, onAction, formatCheckInDate, Modifier.weight(1f), imageContent)
                }
            }
        }
    }
}

private fun previewHistoryState(mode: CheckInHistoryMode = CheckInHistoryMode.Calendar) = CheckInHistoryState(
    "2026-10-14", true, 0,
    HistoryCalendarState(HistoryMonth(2026, 10), mapOf("2026-10-14" to listOf(previewHistoryCheckIn())), "2026-10-14", true),
    HistoryListState(listOf(previewHistoryCheckIn()), page = 1, hasLoaded = true), mode)

@PreviewLightDark @Composable private fun CheckInHistoryCalendarPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(previewHistoryState(), {}, { "14 октября 2026" })
}
@PreviewLightDark @Composable private fun CheckInHistoryListPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(previewHistoryState(CheckInHistoryMode.List), {}, { "14 октября 2026" })
}
@PreviewLightDark @Composable private fun CheckInHistoryGuestPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(previewHistoryState().copy(isLoggedIn = false), {})
}
@PreviewLightDark @Composable private fun CheckInHistoryEmptyPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(previewHistoryState(CheckInHistoryMode.List).copy(list = HistoryListState(hasLoaded = true)), {})
}
@PreviewLightDark @Composable private fun CheckInHistoryErrorPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(previewHistoryState(CheckInHistoryMode.List).copy(list = HistoryListState(hasError = true)), {})
}
@PreviewLightDark @Composable private fun CheckInHistoryLoadingPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(previewHistoryState(CheckInHistoryMode.List).copy(list = HistoryListState(isLoading = true)), {})
}
