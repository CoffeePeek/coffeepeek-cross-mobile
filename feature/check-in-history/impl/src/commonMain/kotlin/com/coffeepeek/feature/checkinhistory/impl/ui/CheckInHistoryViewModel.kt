package com.coffeepeek.feature.checkinhistory.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryMode
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CheckInHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryCalendarState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryListState
import com.coffeepeek.feature.checkinhistory.impl.ui.data.HistoryCalendarContext
import com.coffeepeek.feature.checkinhistory.impl.ui.data.HistoryCalendarProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/** No navigation effects until the screen entry exists; all current intents reduce state. */
internal class CheckInHistoryViewModel private constructor(
    private val repository: CheckInHistoryRepository,
    private val calendarProvider: HistoryCalendarProvider,
    initialIsLoggedIn: Boolean,
    initialSessionGeneration: Int,
    private var calendarContext: HistoryCalendarContext,
) : MviViewModel<CheckInHistoryState, CheckInHistoryAction, Nothing>(
    initialState(calendarContext, initialIsLoggedIn, initialSessionGeneration),
) {
    constructor(
        repository: CheckInHistoryRepository,
        calendarProvider: HistoryCalendarProvider,
        initialIsLoggedIn: Boolean,
        initialSessionGeneration: Int = 0,
    ) : this(repository, calendarProvider, initialIsLoggedIn, initialSessionGeneration, calendarProvider.snapshot())

    private var calendarJob: Job? = null
    private var listJob: Job? = null
    private var calendarRevision = 0L
    private var listRevision = 0L
    private var calendarStale = true
    private var listStale = true
    private var calendarRefreshQueued = false
    private var listRefreshQueued = false
    private var dateSelectedExplicitly = false
    private var firstResume = true
    private var contentGeneration = 0
    private var calendarAvailable = true

    init { loadVisible() }

    override suspend fun handleActionInternal(action: CheckInHistoryAction) {
        when (action) {
            is CheckInHistoryAction.SelectMode -> {
                updateState { copy(mode = action.mode) }
                loadVisible()
            }
            is CheckInHistoryAction.ChangeMonth -> changeMonth(action.offset)
            is CheckInHistoryAction.SelectDate -> {
                if (currentState.calendar.month.containsDate(action.date)) {
                    dateSelectedExplicitly = true
                    updateState { copy(calendar = calendar.copy(selectedDate = action.date)) }
                }
            }
            CheckInHistoryAction.Refresh -> {
                synchronizeCalendar()
                invalidateBoth()
                refreshVisible()
            }
            CheckInHistoryAction.Retry -> {
                synchronizeCalendar()
                loadVisible()
            }
            CheckInHistoryAction.LoadMore -> loadMore(retry = false)
            CheckInHistoryAction.RetryLoadMore -> loadMore(retry = true)
            CheckInHistoryAction.Resume -> {
                val previousCalendarRevision = calendarRevision
                synchronizeCalendar()
                if (firstResume) {
                    firstResume = false
                    if (calendarRevision != previousCalendarRevision && currentState.mode == CheckInHistoryMode.Calendar) loadVisible()
                } else {
                    invalidateBoth()
                    refreshVisible()
                }
            }
            is CheckInHistoryAction.ContentChanged -> {
                if (action.generation <= contentGeneration) return
                contentGeneration = action.generation
                invalidateBoth()
                refreshVisible()
            }
            is CheckInHistoryAction.SessionChanged -> replaceSession(action)
        }
    }

    private fun replaceSession(action: CheckInHistoryAction.SessionChanged) {
        if (action.generation <= currentState.sessionGeneration) return
        cancelCalendar()
        cancelList()
        calendarRevision++
        listRevision++
        calendarStale = true
        listStale = true
        dateSelectedExplicitly = false
        val reset = initialState(calendarContext, action.isLoggedIn, action.generation).copy(mode = currentState.mode)
        updateState { reset }
        synchronizeCalendar()
        if (calendarAvailable) {
            val refreshed = initialState(calendarContext, action.isLoggedIn, action.generation).copy(mode = currentState.mode)
            updateState { refreshed }
        }
        loadVisible()
    }

    private fun synchronizeCalendar() {
        try {
            val next = calendarProvider.snapshot()
            val latestMonth = requireNotNull(HistoryMonth.fromDate(next.today))
            calendarAvailable = true
            if (next.today == calendarContext.today && next.zoneId == calendarContext.zoneId) {
                calendarContext = next
                return
            }
            val zoneChanged = next.zoneId != calendarContext.zoneId
            calendarContext = next
            cancelCalendar()
            calendarRevision++
            calendarStale = true
            val futureMonth = currentState.calendar.month.date(1) > latestMonth.date(1)
            if (zoneChanged || futureMonth) dateSelectedExplicitly = false
            updateState {
                copy(
                    today = next.today,
                    calendar = when {
                        futureMonth -> HistoryCalendarState(latestMonth, selectedDate = next.today)
                        zoneChanged -> HistoryCalendarState(calendar.month)
                        else -> calendar.copy(isLoading = false)
                    },
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            calendarAvailable = false
            cancelCalendar()
            calendarRevision++
            calendarStale = true
            updateState { copy(calendar = calendar.copy(isLoading = false, hasError = isLoggedIn)) }
        }
    }

    private fun invalidateBoth() {
        calendarRevision++
        listRevision++
        calendarStale = true
        listStale = true
        if (calendarJob != null) calendarRefreshQueued = true
        if (listJob != null) listRefreshQueued = true
    }

    private fun changeMonth(offset: Int) {
        if (offset == 0) return
        val target = try { currentState.calendar.month.plusMonths(offset) }
            catch (_: IllegalArgumentException) { return }
        if (target.date(1).substring(0, 7) > currentState.today.substring(0, 7)) return
        cancelCalendar()
        calendarRevision++
        calendarStale = true
        dateSelectedExplicitly = false
        updateState { copy(calendar = HistoryCalendarState(target, hasError = isLoggedIn && !calendarAvailable)) }
        if (currentState.mode == CheckInHistoryMode.Calendar) loadCalendar()
    }

    private fun loadVisible() {
        if (!currentState.isLoggedIn) return
        when (currentState.mode) {
            CheckInHistoryMode.Calendar -> if (calendarStale) loadCalendar()
            CheckInHistoryMode.List -> if (listStale) loadList(reset = true)
        }
    }

    private fun refreshVisible() {
        if (currentState.mode == CheckInHistoryMode.List && currentState.list.isLoadingMore) cancelList()
        loadVisible()
    }

    private fun loadCalendar() {
        // A failed snapshot must never fall back to querying the previous zone.
        if (!currentState.isLoggedIn || !calendarAvailable || calendarJob != null) return
        val generation = currentState.sessionGeneration
        val revision = calendarRevision
        val month = currentState.calendar.month
        val context = calendarContext
        updateState { copy(calendar = calendar.copy(isLoading = true, hasError = false)) }
        val work = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val job = currentCoroutineContext().job
            var cancelled = false
            try {
                val items = repository.period(context.period(month)).getOrThrow().distinctBy { it.id }
                val grouped = items.groupBy { entry ->
                    val date = requireNotNull(context.localDate(entry.effectiveVisitedAt))
                    requireNotNull(HistoryMonth.fromDate(date))
                    date
                }.filterKeys(month::containsDate)
                currentCoroutineContext().ensureActive()
                if (calendarJob !== job || currentState.sessionGeneration != generation || calendarRevision != revision) return@launch
                val previous = currentState.calendar
                val selection = when {
                    (dateSelectedExplicitly || previous.hasLoaded) && previous.selectedDate?.let(month::containsDate) == true -> previous.selectedDate
                    month.containsDate(context.today) && context.today in grouped -> context.today
                    grouped.isNotEmpty() -> grouped.keys.maxOrNull()
                    month.containsDate(context.today) -> context.today
                    else -> null
                }
                calendarStale = false
                updateState { copy(calendar = calendar.copy(checkInsByDate = grouped, selectedDate = selection, hasLoaded = true)) }
            } catch (failure: CancellationException) {
                cancelled = true
                throw failure
            } catch (_: Exception) {
                if (calendarJob === job && currentState.sessionGeneration == generation && calendarRevision == revision) {
                    updateState { copy(calendar = calendar.copy(hasError = true)) }
                }
            } finally {
                if (calendarJob === job) {
                    calendarJob = null
                    updateState { copy(calendar = calendar.copy(isLoading = false)) }
                    val queued = calendarRefreshQueued
                    calendarRefreshQueued = false
                    if (queued && !cancelled && currentCoroutineContext().isActive && currentState.mode == CheckInHistoryMode.Calendar) loadVisible()
                }
            }
        }
        calendarJob = work
        work.start()
    }

    private fun loadMore(retry: Boolean) {
        val state = currentState.list
        if (currentState.mode != CheckInHistoryMode.List || !state.hasLoaded || !state.hasMore || listStale) return
        if (state.hasLoadMoreError && !retry) return
        loadList(reset = false)
    }

    private fun loadList(reset: Boolean) {
        if (!currentState.isLoggedIn || listJob != null) return
        val generation = currentState.sessionGeneration
        val revision = listRevision
        val number = if (reset) 1 else currentState.list.page + 1
        updateState {
            copy(list = list.copy(isLoading = reset, isLoadingMore = !reset, hasError = false, hasLoadMoreError = false))
        }
        val work = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val job = currentCoroutineContext().job
            var cancelled = false
            try {
                val result = repository.page(number, 20).getOrThrow()
                check(result.page == number && result.pageSize == 20) { "Unexpected history page" }
                check(result.items.isNotEmpty() || !result.hasMore) { "Missing history page" }
                currentCoroutineContext().ensureActive()
                if (listJob !== job || currentState.sessionGeneration != generation || listRevision != revision) return@launch
                listStale = false
                updateState {
                    copy(list = list.copy(
                        checkIns = (if (reset) result.items else list.checkIns + result.items).distinctBy { it.id },
                        page = result.page, hasMore = result.hasMore, hasLoaded = true,
                    ))
                }
            } catch (failure: CancellationException) {
                cancelled = true
                throw failure
            } catch (_: Exception) {
                if (listJob === job && currentState.sessionGeneration == generation && listRevision == revision) {
                    updateState { copy(list = list.copy(hasError = reset, hasLoadMoreError = !reset)) }
                }
            } finally {
                if (listJob === job) {
                    listJob = null
                    updateState { copy(list = list.copy(isLoading = false, isLoadingMore = false)) }
                    val queued = listRefreshQueued
                    listRefreshQueued = false
                    if (queued && !cancelled && currentCoroutineContext().isActive && currentState.mode == CheckInHistoryMode.List) loadVisible()
                }
            }
        }
        listJob = work
        work.start()
    }

    private fun cancelCalendar() {
        val previous = calendarJob
        calendarJob = null
        calendarRefreshQueued = false
        previous?.cancel()
    }

    private fun cancelList() {
        val previous = listJob
        listJob = null
        listRefreshQueued = false
        previous?.cancel()
        updateState { copy(list = list.copy(isLoading = false, isLoadingMore = false)) }
    }

    companion object {
        private fun initialState(context: HistoryCalendarContext, loggedIn: Boolean, generation: Int): CheckInHistoryState {
            require(generation >= 0)
            val month = requireNotNull(HistoryMonth.fromDate(context.today))
            return CheckInHistoryState(context.today, loggedIn, generation, HistoryCalendarState(month, selectedDate = context.today))
        }
    }
}
