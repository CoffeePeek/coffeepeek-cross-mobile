package com.coffeepeek.feature.checkinhistory.impl

import androidx.compose.runtime.*
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
    image: HistoryImageContent,
): ProfileActivityEntry = DefaultProfileActivityEntry(shops, homes, snapshot, image)

private class DefaultProfileActivityEntry(
    private val shops: CheckInHistoryRepository, private val homes: HomeHistoryReader,
    private val snapshot: () -> CheckInHistoryCalendarSnapshot, private val image: HistoryImageContent,
) : ProfileActivityEntry {
    @Composable override fun Calendar(isLoggedIn: Boolean, selectedDate: String?, onSelectDate: (String?) -> Unit,
        expanded: Boolean, title: @Composable (String) -> Unit) {
        val model = activityModel(weekly = !expanded, loggedIn = isLoggedIn)
        if (!expanded) LaunchedEffect(model) { model.onAction(ProfileActivityAction.SyncVisibleWeek) }
        key(model) {
            val state by model.state.collectAsStateWithLifecycle()
            ProfileActivityCalendarContent(state, model::onAction, image, selectedDate, onSelectDate, expanded, title)
        }
    }

    @Composable private fun activityModel(weekly: Boolean, loggedIn: Boolean): ProfileActivityViewModel {
        val model = viewModel(key = if (weekly) "profile-activity-week" else "profile-activity-calendar") {
            ProfileActivityViewModel(shops, homes, snapshot, weekly, loggedIn)
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.onAction(ProfileActivityAction.Resume) }
        return model
    }
}
