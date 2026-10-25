package com.coffeepeek.feature.checkinhistory.impl.ui.compose

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekScreenInsets
import com.coffeepeek.core.designsystem.component.coffeePeekScreenBottomPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
        Scaffold(contentWindowInsets = if (LocalCoffeePeekScreenInsets.current.extendsToBottom) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            topBar = { CoffeePeekTopBar(stringResource(Res.string.history_title), stringResource(Res.string.history_back),
                onBack = { onAction(CombinedHistoryAction.Back) }) },
            floatingActionButton = {
                if (state.isLoggedIn) {
                    val description = stringResource(Res.string.history_add)
                    FloatingActionButton({ onAction(CombinedHistoryAction.ShowCreate) }, shape = CircleShape,
                        modifier = createButtonModifier.coffeePeekScreenBottomPadding().semantics(mergeDescendants = true) { contentDescription = description },
                        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                        Icon(CoffeePeekIcons.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
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

@Preview @Composable private fun CombinedHistoryPreviewLight() = CombinedHistoryPreview(false)
@Preview @Composable private fun CombinedHistoryPreviewDark() = CombinedHistoryPreview(true)

@Composable private fun CombinedHistoryPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CheckInHistoryScreenContent(previewCombinedHistoryState(), {}, formatCheckInTime = { "13:00" })
}
@Preview @Composable private fun HistoryGuestPreviewLight() = HistoryGuestPreview(false)
@Preview @Composable private fun HistoryGuestPreviewDark() = HistoryGuestPreview(true)

@Composable private fun HistoryGuestPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CheckInHistoryScreenContent(previewCombinedHistoryState().copy(isLoggedIn = false), {})
}
@Preview @Composable private fun HistoryEmptyPreviewLight() = HistoryEmptyPreview(false)
@Preview @Composable private fun HistoryEmptyPreviewDark() = HistoryEmptyPreview(true)

@Composable private fun HistoryEmptyPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CheckInHistoryScreenContent(CombinedHistoryState("2026-10-14", HistoryMonth(2026, 10), true, 0), {})
}
@Preview @Composable private fun HistoryErrorPreviewLight() = HistoryErrorPreview(false)
@Preview @Composable private fun HistoryErrorPreviewDark() = HistoryErrorPreview(true)

@Composable private fun HistoryErrorPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CheckInHistoryScreenContent(previewCombinedHistoryState().copy(homes = CombinedHistoryPageState(hasError = true)), {})
}
@Preview @Composable private fun HistoryLoadingPreviewLight() = HistoryLoadingPreview(false)
@Preview @Composable private fun HistoryLoadingPreviewDark() = HistoryLoadingPreview(true)

@Composable private fun HistoryLoadingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CheckInHistoryScreenContent(previewCombinedHistoryState().copy(shops = CombinedHistoryPageState(isLoading = true)), {})
}
