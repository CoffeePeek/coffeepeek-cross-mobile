package com.coffeepeek.feature.checkinhistory.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.impl.ui.ProfileActivityViewModel
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryTimelineItem
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.ProfileActivityAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.ProfileActivityEvent
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.ProfileActivityState
import com.coffeepeek.feature.checkinhistory.impl.ui.data.historyCalendarPhotos
import com.coffeepeek.feature.checkinhistory.impl.ui.data.profileActivityDates
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun createIosProfileActivitySession(
    shops: CheckInHistoryRepository, homes: HomeHistoryReader, calendar: () -> CheckInHistoryCalendarSnapshot,
    expanded: Boolean,
): IosProfileActivitySession = IosProfileActivitySession(ProfileActivityViewModel(shops, homes, calendar, !expanded, true), expanded)

class ProfileActivitySnapshot internal constructor(state: ProfileActivityState, expanded: Boolean) {
    val year = state.calendar.month.year
    val month = state.calendar.month.month
    val today = state.calendar.today
    val canGoNext = state.calendar.canGoNextMonth
    val canGoPrevious = state.calendar.month != HistoryMonth(1, 1)
    val loading = state.loading
    val failed = state.failed
    val selected = state.calendar.selectedDate
    val rows = state.selectedRows.map(::ProfileActivityRow)
    val days = profileActivityDates(today, state.calendar.month, expanded).map { date ->
        ProfileActivityDay(date, state.calendar)
    }
}

class ProfileActivityDay internal constructor(val date: String?, calendar: CombinedHistoryState) {
    val homeCount = date?.let { calendar.homeCalendar.content[it] } ?: 0
    val shopCount = date?.let { calendar.calendarCounts[it] } ?: 0L
    val photo = date?.let { historyCalendarPhotos(calendar, it, maxPhotos = 1).firstOrNull()?.url }
    val selected = date != null && calendar.selectedDate == date
    val today = date == calendar.today
    val enabled = date != null && date <= calendar.today
}

class ProfileActivityRow internal constructor(row: HistoryTimelineItem) {
    val key = row.key
    val timestamp = row.timestamp
    val home = row is HistoryTimelineItem.Home
    val title = when (row) {
        is HistoryTimelineItem.Shop -> row.entry.shopName.takeIf(String::isNotBlank)
        is HistoryTimelineItem.Home -> null
    }
    val drink = when (row) {
        is HistoryTimelineItem.Shop -> row.entry.drink?.customName?.takeIf(String::isNotBlank) ?: row.entry.drink?.nameRu
        is HistoryTimelineItem.Home -> row.entry.customDrinkName?.takeIf(String::isNotBlank) ?: row.entry.drinkName
    }
    val photo = when (row) {
        is HistoryTimelineItem.Shop -> (row.entry.photoCalendarUrls ?: row.entry.photoThumbnailUrls ?: row.entry.photoUrls)
            .firstOrNull(String::isNotBlank)
        is HistoryTimelineItem.Home -> row.entry.photos.firstNotNullOfOrNull { it.calendarUrl?.takeIf(String::isNotBlank) }
    }
}

/** Shared history rules stay in ProfileActivityViewModel; UIKit owns the lifetime and destinations. */
class IosProfileActivitySession internal constructor(private val model: ProfileActivityViewModel, private val expanded: Boolean) {
    private val store = ViewModelStore().apply { put("profile-activity", model) }
    private val owner = Any()

    fun current() = ProfileActivitySnapshot(model.state.value, expanded)

    fun observe(onState: (ProfileActivitySnapshot) -> Unit, onShop: (String) -> Unit, onHistory: () -> Unit): List<Job> {
        return listOf(model.viewModelScope.launch { model.state.collect { onState(current()) } },
            model.viewModelScope.launch { model.events.collect { event ->
                when (val destination = event.forOwner(owner)) {
                    is ProfileActivityEvent.OpenShop -> onShop(destination.slug)
                    ProfileActivityEvent.OpenHistory, ProfileActivityEvent.OpenWeek -> onHistory()
                    null -> Unit
                }
            } })
    }

    fun changeMonth(offset: Int) = model.onAction(ProfileActivityAction.ChangeMonth(offset))
    fun selectDay(date: String) = model.onAction(ProfileActivityAction.SelectDate(date))
    fun openWeek() = model.onAction(ProfileActivityAction.OpenWeek(owner))
    fun openRow(key: String) = model.onAction(ProfileActivityAction.OpenRow(key, owner))
    fun retry() = model.onAction(ProfileActivityAction.Retry)
    fun visible() = model.onAction(ProfileActivityAction.Resume)
    fun close() = store.clear()
}
