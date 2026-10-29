package com.coffeepeek.feature.checkinhistory.impl

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.checkinhistory.api.*
import com.coffeepeek.feature.checkinhistory.domain.repository.*
import com.coffeepeek.feature.checkinhistory.impl.ui.ProfileActivityViewModel
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.component.HistoryImageContent
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*

fun createProfileActivityEntry(
    shops: CheckInHistoryRepository, homes: HomeHistoryReader, snapshot: () -> CheckInHistoryCalendarSnapshot,
    image: HistoryImageContent, timeLabel: (String) -> String,
): ProfileActivityEntry = DefaultProfileActivityEntry(shops, homes, snapshot, image, timeLabel)

private class DefaultProfileActivityEntry(
    private val shops: CheckInHistoryRepository, private val homes: HomeHistoryReader,
    private val snapshot: () -> CheckInHistoryCalendarSnapshot, private val image: HistoryImageContent,
    private val timeLabel: (String) -> String,
) : ProfileActivityEntry {
    @Composable override fun Calendar(isLoggedIn: Boolean, onOpenShop: (String) -> Unit, onOpenHistory: () -> Unit) {
        Surface(false, isLoggedIn, Modifier) { event ->
            when (event) {
                is ProfileActivityEvent.OpenShop -> onOpenShop(event.slug)
                ProfileActivityEvent.OpenHistory -> onOpenHistory()
                ProfileActivityEvent.OpenWeek -> Unit
            }
        }
    }

    @Composable override fun Week(isLoggedIn: Boolean, onOpen: () -> Unit) {
        Surface(true, isLoggedIn, Modifier) { if (it == ProfileActivityEvent.OpenWeek) onOpen() }
    }

    @Composable private fun Surface(weekly: Boolean, loggedIn: Boolean, modifier: Modifier, onEvent: (ProfileActivityEvent) -> Unit) {
        val model = viewModel(key = if (weekly) "profile-activity-week" else "profile-activity-calendar") {
            ProfileActivityViewModel(shops, homes, snapshot, weekly, loggedIn)
        }
        val state by model.state.collectAsStateWithLifecycle()
        val event by rememberUpdatedState(onEvent)
        val owner = remember(model) { Any() }
        val dispatch: (ProfileActivityAction) -> Unit = { action ->
            model.onAction(when (action) {
                is ProfileActivityAction.OpenRow -> action.copy(owner = owner)
                is ProfileActivityAction.OpenWeek -> action.copy(owner = owner)
                else -> action
            })
        }
        LaunchedEffect(model) { model.events.collect { it.forOwner(owner)?.let(event) } }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.onAction(ProfileActivityAction.Resume) }
        if (weekly) ProfileActivityWeekContent(state, dispatch, image, modifier)
        else ProfileActivityCalendarContent(state, dispatch, image, timeLabel, modifier)
    }
}
