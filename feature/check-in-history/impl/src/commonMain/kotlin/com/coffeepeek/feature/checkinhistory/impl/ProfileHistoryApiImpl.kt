package com.coffeepeek.feature.checkinhistory.impl

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.checkinhistory.api.*
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn
import com.coffeepeek.feature.checkinhistory.domain.repository.*
import com.coffeepeek.feature.checkinhistory.impl.ui.CombinedHistoryViewModel
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.ProfileHistoryListContent
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*

fun createProfileHistoryEntry(
    repository: CheckInHistoryRepository,
    homeReader: HomeHistoryReader,
    calendarSnapshot: () -> CheckInHistoryCalendarSnapshot,
    imageContent: @Composable (String, Modifier, ContentScale) -> Unit,
    formatDate: (String) -> String,
    modalContent: @Composable (@Composable () -> Unit) -> Unit = { it() },
    highQualityImage: (@Composable (String, String, Modifier, ContentScale) -> Unit)? = null,
    shopTimelineContent: (@Composable (PersonalCheckIn, () -> Unit) -> Unit)? = null,
    homeTimelineContent: (@Composable (HomeHistoryEntry, () -> Unit) -> Unit)? = null,
): ProfileHistoryEntry {
    return DefaultProfileHistoryEntry(repository, homeReader, calendarSnapshot,
        imageContent, formatDate, modalContent, highQualityImage, shopTimelineContent, homeTimelineContent)
}

private class DefaultProfileHistoryEntry(
    private val repository: CheckInHistoryRepository,
    private val homeReader: HomeHistoryReader,
    private val calendarSnapshot: () -> CheckInHistoryCalendarSnapshot,
    private val image: HistoryImageContent,
    private val formatDate: (String) -> String,
    private val modal: @Composable (@Composable () -> Unit) -> Unit,
    private val highQuality: HistoryHighQualityImage?,
    private val shopCard: (@Composable (PersonalCheckIn, () -> Unit) -> Unit)?,
    private val homeCard: (@Composable (HomeHistoryEntry, () -> Unit) -> Unit)?,
) : ProfileHistoryEntry {
    @Composable
    override fun Content(sessionGeneration: Int, contentGeneration: Int,
        showAllRequested: Boolean, onShowAllConsumed: () -> Unit,
        onOpenHome: (String) -> Unit, onOpenShop: (String) -> Unit, onOpenCheckIn: (String) -> Unit,
        onOpenPhoto: (List<String>, List<String>, Int) -> Unit,
        content: @Composable (@Composable () -> Unit, @Composable (@Composable () -> Unit) -> Unit, Int) -> Unit) {
        val model = viewModel(key = "profile-combined-history") {
            CombinedHistoryViewModel(repository, homeReader, historyCalendarProvider(calendarSnapshot),
                true, sessionGeneration, contentGeneration)
        }
        ObserveProfileHistory(model, sessionGeneration, contentGeneration) { event ->
            when (event) {
                is CheckInHistoryEvent.OpenHome -> onOpenHome(event.id)
                is CheckInHistoryEvent.OpenShop -> onOpenShop(event.slug)
                is CheckInHistoryEvent.OpenCheckIn -> onOpenCheckIn(event.id)
                is CheckInHistoryEvent.OpenPhoto -> onOpenPhoto(event.urls, event.originalUrls, event.index)
                else -> Unit
            }
        }
        val state by model.state.collectAsStateWithLifecycle()
        CompositionLocalProvider(LocalHistoryHighQualityImage provides highQuality,
            LocalHistoryShopTimelineContent provides shopCard, LocalHistoryHomeTimelineContent provides homeCard) {
            ProfileHistoryScope(state.visibleTo(sessionGeneration, true), model::onAction, image, formatDate, modal,
                showAllRequested, onShowAllConsumed, content)
        }
    }
}

@Composable
private fun ObserveProfileHistory(model: CombinedHistoryViewModel, generation: Int, contentGeneration: Int,
    onEvent: (CheckInHistoryEvent) -> Unit) {
    val currentEvent by rememberUpdatedState(onEvent)
    val currentGeneration by rememberUpdatedState(generation)
    LaunchedEffect(model) {
        model.events.collect { envelope ->
            if (envelope.isDeliverableTo(currentGeneration, true)) currentEvent(envelope.event)
        }
    }
    LaunchedEffect(model, generation) { model.onAction(CombinedHistoryAction.SessionChanged(generation, true)) }
    LaunchedEffect(model, contentGeneration) { model.onAction(CombinedHistoryAction.ContentChanged(contentGeneration)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.onAction(CombinedHistoryAction.Resume) }
}

@Composable
private fun ProfileHistoryScope(state: CombinedHistoryState, onAction: (CombinedHistoryAction) -> Unit,
    image: HistoryImageContent, formatDate: (String) -> String, modal: @Composable (@Composable () -> Unit) -> Unit,
    showAllRequested: Boolean, onShowAllConsumed: () -> Unit,
    content: @Composable (@Composable () -> Unit, @Composable (@Composable () -> Unit) -> Unit, Int) -> Unit) {
    var expanded by rememberSaveable(state.sessionGeneration) { mutableStateOf(false) }
    var selectionRevision by rememberSaveable(state.sessionGeneration) { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val dispatch: (CombinedHistoryAction) -> Unit = { action ->
        if (action is CombinedHistoryAction.SelectDate) selectionRevision++
        onAction(action)
    }
    LaunchedEffect(selectionRevision, state.selectedDate) { listState.scrollToItem(0) }
    LaunchedEffect(showAllRequested) {
        if (showAllRequested) {
            dispatch(CombinedHistoryAction.SelectDate(null))
            onShowAllConsumed()
        }
    }
    state.selectedShop?.entry?.let { entry ->
        modal {
            HistoryShopDetailSheet(entry, formatDate(entry.createdAt),
                { dispatch(CombinedHistoryAction.DismissShop) }, { dispatch(CombinedHistoryAction.OpenSelectedShop) },
                { dispatch(CombinedHistoryAction.OpenSelectedCheckIn) }, { dispatch(CombinedHistoryAction.OpenPhoto(it)) }, image)
        }
    }
    content({ ProfileHistoryCalendarCard(state, expanded, { expanded = !expanded }, dispatch, image) },
        { header -> ProfileHistoryListContent(state, dispatch, listState, header, image, formatDate) }, selectionRevision)
}
