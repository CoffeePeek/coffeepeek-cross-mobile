package com.coffeepeek.feature.checkinhistory.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.impl.ui.CombinedHistoryViewModel
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistorySource
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryTimelineItem
import com.coffeepeek.feature.checkinhistory.impl.ui.data.timelinePhotos
import kotlinx.coroutines.launch

fun createIosOwnProfileHistorySession(shops: CheckInHistoryRepository, homes: HomeHistoryReader,
    calendar: () -> CheckInHistoryCalendarSnapshot): IosOwnProfileHistorySession =
    IosOwnProfileHistorySession(CombinedHistoryViewModel(shops, homes, historyCalendarProvider(calendar), true))

/** One owner history powers the calendar and the complete mixed timeline, including private records. */
class IosOwnProfileHistorySession internal constructor(private val model: CombinedHistoryViewModel) {
    private val store = ViewModelStore().apply { put("own-profile-history", model) }
    private var closed = false
    fun current() = OwnProfileHistorySnapshot(model.state.value)
    fun activity(expanded: Boolean) = ProfileActivitySnapshot(model.state.value, expanded)
    fun observe(changed: () -> Unit) = model.viewModelScope.launch { model.state.collect { changed() } }
    fun more() = send(CombinedHistoryAction.LoadMore)
    fun refresh() = send(CombinedHistoryAction.Refresh)
    fun resume() = send(CombinedHistoryAction.Resume)
    fun changeMonth(offset: Int) = send(CombinedHistoryAction.ChangeMonth(offset))

    fun selectDay(date: String?) = send(CombinedHistoryAction.SelectDate(date))

    fun retry() {
        val state = model.state.value
        if (state.isCalendarUnavailable) {
            refresh()
            return
        }
        if (state.selectedDate == null && state.shops.hasLoadMoreError) send(CombinedHistoryAction.RetryLoadMore(HistorySource.Shop))
        else if (if (state.selectedDate == null) state.shops.hasError else state.shopCalendar.hasError) {
            send(CombinedHistoryAction.RetryTimeline(HistorySource.Shop))
        }
        if (state.homes.hasLoadMoreError) send(CombinedHistoryAction.RetryLoadMore(HistorySource.Home))
        else if (state.homes.hasError) send(CombinedHistoryAction.RetryTimeline(HistorySource.Home))
    }

    fun retryCalendar() {
        HistorySource.entries.forEach { send(CombinedHistoryAction.RetryCalendar(it)) }
    }

    fun close() {
        closed = true
        store.clear()
    }

    private fun send(action: CombinedHistoryAction) { if (!closed) model.onAction(action) }
}

class OwnProfileHistorySnapshot internal constructor(state: CombinedHistoryState) {
    val rows = state.timeline.map(::OwnProfileHistoryRow)
    val selectedDate = state.selectedDate
    val loading = state.isTimelineLoading
    val loadingMore = state.shops.isLoadingMore || state.homes.isLoadingMore
    val failed = state.isCalendarUnavailable ||
        (if (selectedDate == null) state.shops.hasError else state.shopCalendar.hasError) || state.homes.hasError
    val moreFailed = (selectedDate == null && state.shops.hasLoadMoreError) || state.homes.hasLoadMoreError
    val more = state.hasMore
}

class OwnProfileHistoryRow internal constructor(item: HistoryTimelineItem) {
    val key = item.key
    val timestamp = item.timestamp
    val shop = (item as? HistoryTimelineItem.Shop)?.entry
    val home = (item as? HistoryTimelineItem.Home)?.entry
    private val sources = when (item) {
        is HistoryTimelineItem.Shop -> item.entry.timelinePhotos()
        is HistoryTimelineItem.Home -> item.entry.timelinePhotos()
    }
    val originals = sources.originals
    val previews = sources.previews
    fun displayPhoto(index: Int) = sources.display(index)
    fun canOpenPhoto(index: Int) = sources.canOpen(index)
    fun fallbackPhoto(index: Int) = sources.upgrade(index)
}
