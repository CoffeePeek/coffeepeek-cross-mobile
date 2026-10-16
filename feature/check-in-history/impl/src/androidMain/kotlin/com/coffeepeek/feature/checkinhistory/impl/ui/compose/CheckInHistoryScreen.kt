package com.coffeepeek.feature.checkinhistory.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.CombinedHistoryViewModel
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CheckInHistoryScreen(viewModel: CombinedHistoryViewModel, sessionGeneration: Int,
    isLoggedIn: Boolean, contentGeneration: Int, formatCheckInDate: (String) -> String,
    formatCheckInTime: (String) -> String, imageContent: HistoryImageContent,
    createButtonModifier: Modifier, createPickerModifier: Modifier,
    modalContent: @Composable (@Composable () -> Unit) -> Unit, onEvent: (CheckInHistoryEvent) -> Unit) {
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
        viewModel.onAction(CombinedHistoryAction.SessionChanged(sessionGeneration, isLoggedIn))
    }
    LaunchedEffect(viewModel, contentGeneration) { viewModel.onAction(CombinedHistoryAction.ContentChanged(contentGeneration)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(CombinedHistoryAction.Resume) }
    CheckInHistoryScreenContent(state.visibleTo(sessionGeneration, isLoggedIn), viewModel::onAction,
        formatCheckInDate, formatCheckInTime, imageContent, createButtonModifier, createPickerModifier, modalContent)
}

@Composable
internal fun CheckInHistoryScreenContent(state: CombinedHistoryState, onAction: (CombinedHistoryAction) -> Unit,
    formatCheckInDate: (String) -> String = { it.substringBefore('T') },
    formatCheckInTime: (String) -> String = { it.substringAfter('T').take(5) },
    imageContent: HistoryImageContent? = null, createButtonModifier: Modifier = Modifier,
    createPickerModifier: Modifier = Modifier,
    modalContent: @Composable (@Composable () -> Unit) -> Unit = { it() }) {
    key(state.sessionGeneration) {
        if (state.isLoggedIn) {
            state.selectedShop?.entry?.let { entry ->
                modalContent {
                    HistoryShopDetailSheet(entry, formatCheckInDate(entry.createdAt),
                        { onAction(CombinedHistoryAction.DismissShop) }, { onAction(CombinedHistoryAction.OpenSelectedShop) },
                        { onAction(CombinedHistoryAction.OpenPhoto(it)) }, imageContent)
                }
            }
            if (state.showCreate) modalContent {
                HistoryCreateSheet({ onAction(CombinedHistoryAction.DismissCreate) },
                    { onAction(CombinedHistoryAction.Create(it)) }, createPickerModifier)
            }
        }
        Scaffold(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            topBar = { CoffeePeekTopBar(stringResource(Res.string.history_title), stringResource(Res.string.history_back),
                onBack = { onAction(CombinedHistoryAction.Back) }) },
            floatingActionButton = {
                if (state.isLoggedIn) FloatingActionButton({ onAction(CombinedHistoryAction.ShowCreate) }, shape = CircleShape,
                    modifier = createButtonModifier,
                    containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Icon(CoffeePeekIcons.Add, stringResource(Res.string.history_add), Modifier.size(28.dp))
                }
            }, containerColor = MaterialTheme.colorScheme.background) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                HistoryKindFilterControl(state.filter, { onAction(CombinedHistoryAction.SelectFilter(it)) },
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                if (!state.isLoggedIn) HistoryGuestPrompt({ onAction(CombinedHistoryAction.SignIn) })
                else HistoryTimelineContent(state, onAction, formatCheckInTime, Modifier.weight(1f), imageContent)
            }
        }
    }
}

internal fun previewCombinedHistoryState(): CombinedHistoryState {
    val shop = HistoryTimelineItem.Shop(previewHistoryCheckIn(), "2026-10-14")
    val home = HistoryTimelineItem.Home(previewHomeHistoryEntry())
    return CombinedHistoryState("2026-10-14", HistoryMonth(2026, 10), true, 0,
        shopCalendar = CombinedHistoryLoadState(mapOf(shop.date to listOf(shop)), hasLoaded = true),
        homeCalendar = CombinedHistoryLoadState(mapOf(home.date to 1), hasLoaded = true),
        shops = CombinedHistoryPageState(listOf(shop), page = 1, hasLoaded = true),
        homes = CombinedHistoryPageState(listOf(home), page = 1, hasLoaded = true))
}

@PreviewLightDark @Composable private fun CombinedHistoryPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(previewCombinedHistoryState(), {}, formatCheckInTime = { "13:00" })
}
@PreviewLightDark @Composable private fun HistoryGuestPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(previewCombinedHistoryState().copy(isLoggedIn = false), {})
}
@PreviewLightDark @Composable private fun HistoryEmptyPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(CombinedHistoryState("2026-10-14", HistoryMonth(2026, 10), true, 0), {})
}
@PreviewLightDark @Composable private fun HistoryErrorPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(previewCombinedHistoryState().copy(homes = CombinedHistoryPageState(hasError = true)), {})
}
@PreviewLightDark @Composable private fun HistoryLoadingPreview() = CoffeePeekTheme {
    CheckInHistoryScreenContent(previewCombinedHistoryState().copy(shops = CombinedHistoryPageState(isLoading = true)), {})
}
