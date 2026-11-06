package com.coffeepeek.feature.checkinhistory.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.checkinhistory.api.CheckInHistoryCalendarSnapshot
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.domain.repository.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.*
import kotlinx.coroutines.*

/** Separate week/month models: browsing a month cannot replace the profile's current-week preview. */
internal class ProfileActivityViewModel(
    private val shops: CheckInHistoryRepository, private val homes: HomeHistoryReader,
    private val snapshot: () -> CheckInHistoryCalendarSnapshot, private val weekly: Boolean,
    loggedIn: Boolean, initial: CheckInHistoryCalendarSnapshot = snapshot(),
) : MviViewModel<ProfileActivityState, ProfileActivityAction, ProfileActivityEventEnvelope>(
    ProfileActivityState(CombinedHistoryState(initial.today, requireNotNull(HistoryMonth.fromDate(initial.today)), loggedIn, 0)),
) {
    private var revision = 0L
    private val reads = mutableListOf<Job>()
    private var firstResume = true

    init { reload(initial) }

    override suspend fun handleActionInternal(action: ProfileActivityAction) {
        when (action) {
            is ProfileActivityAction.ChangeMonth -> changeMonth(action.offset)
            is ProfileActivityAction.SelectDate -> selectDate(action.date)
            is ProfileActivityAction.OpenRow -> openRow(action.key, action.owner)
            ProfileActivityAction.Today -> refresh(resetToToday = true)
            ProfileActivityAction.Retry -> refresh()
            ProfileActivityAction.Resume -> {
                if (firstResume) firstResume = false else refresh()
            }
            is ProfileActivityAction.OpenWeek -> if (currentState.calendar.isLoggedIn) {
                sendEvent(ProfileActivityEventEnvelope(ProfileActivityEvent.OpenWeek, action.owner))
            }
        }
    }

    private fun updateCalendar(transform: CombinedHistoryState.() -> CombinedHistoryState) {
        updateState { copy(calendar = calendar.transform()) }
    }

    private fun selectDate(date: String) {
        val state = currentState.calendar
        if (weekly || !state.month.containsDate(date) || date > state.today) return
        updateCalendar { copy(selectedDate = date.takeUnless { it == selectedDate }) }
    }

    private suspend fun openRow(key: String, owner: Any) {
        if (!currentState.calendar.isLoggedIn || currentState.loading || currentState.failed) return
        when (val row = currentState.selectedRows.firstOrNull { it.key == key }) {
            is HistoryTimelineItem.Shop -> row.entry.shop?.slug?.takeIf(String::isNotBlank)
                ?.let { sendEvent(ProfileActivityEventEnvelope(ProfileActivityEvent.OpenShop(it), owner)) }
                ?: sendEvent(ProfileActivityEventEnvelope(ProfileActivityEvent.OpenHistory, owner))
            is HistoryTimelineItem.Home -> sendEvent(ProfileActivityEventEnvelope(ProfileActivityEvent.OpenHistory, owner))
            null -> Unit
        }
    }

    private fun changeMonth(offset: Int) {
        if (weekly || offset == 0) return
        val month = try { currentState.calendar.month.plusMonths(offset) } catch (_: IllegalArgumentException) { return }
        if (month.date(1) > currentState.calendar.today) return
        updateCalendar { copy(month = month, selectedDate = null,
            shopCalendar = CombinedHistoryLoadState(emptyMap()), homeCalendar = CombinedHistoryLoadState(emptyMap()),
            homeCalendarPhotos = emptyMap(), homes = CombinedHistoryPageState()) }
        refresh()
    }

    private fun refresh(resetToToday: Boolean = false) {
        try {
            val next = snapshot()
            val latest = requireNotNull(HistoryMonth.fromDate(next.today))
            updateCalendar { copy(today = next.today, isCalendarUnavailable = false,
                month = if (resetToToday || weekly || month.date(1) > next.today) latest else month,
                selectedDate = if (resetToToday) next.today else selectedDate?.takeIf { it <= next.today }) }
            reload(next)
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) {
            cancelReads()
            updateCalendar { copy(isCalendarUnavailable = true,
                shopCalendar = shopCalendar.copy(isLoading = false), homeCalendar = homeCalendar.copy(isLoading = false)) }
        }
    }

    private fun cancelReads() {
        revision++
        reads.forEach(Job::cancel)
        reads.clear()
    }

    private fun reload(context: CheckInHistoryCalendarSnapshot) {
        cancelReads()
        if (!currentState.calendar.isLoggedIn) return
        val range = activityRange(context.today, currentState.calendar.month, weekly)
        loadShop(context, range)
        loadHome(range)
    }

    private fun loadShop(context: CheckInHistoryCalendarSnapshot, range: HomeHistoryDateRange) {
        updateCalendar { copy(shopCalendar = shopCalendar.copy(isLoading = true, hasError = false)) }
        read(load = {
            val bounds = context.datePeriod(range.from, shiftActivityDate(range.to, 1))
            shops.period(HistoryPeriod(bounds.fromInclusive, bounds.toExclusive)).getOrThrow().distinctBy { it.id }
                .map { HistoryTimelineItem.Shop(it, requireNotNull(context.localDate(it.effectiveVisitedAt))) }
                .filter { it.date in range.from..range.to }.groupBy { it.date }
        }, success = { rows -> updateCalendar { copy(shopCalendar = CombinedHistoryLoadState(rows, hasLoaded = true)) } },
            failure = { updateCalendar { copy(shopCalendar = shopCalendar.copy(hasError = true, isLoading = false)) } })
    }

    private fun loadHome(range: HomeHistoryDateRange) {
        updateCalendar { copy(homeCalendar = homeCalendar.copy(isLoading = true, hasError = false)) }
        read(load = {
            val days = homes.calendar(range).getOrThrow().filter { it.preparedOn in range.from..range.to }
            check(days.distinctBy { it.preparedOn }.size == days.size)
            days to readActivityHomeRows(homes, range)
        }, success = { (days, rows) -> updateCalendar { copy(
            homeCalendar = CombinedHistoryLoadState(days.associate { it.preparedOn to it.brewCount }, hasLoaded = true),
            homeCalendarPhotos = days.associate { it.preparedOn to it.photos },
            homes = CombinedHistoryPageState(items = rows.map { HistoryTimelineItem.Home(it) }, hasLoaded = true)) } },
            failure = { updateCalendar { copy(homeCalendar = homeCalendar.copy(hasError = true, isLoading = false)) } })
    }

    private fun <T> read(load: suspend () -> T, success: (T) -> Unit, failure: () -> Unit) {
        val mine = revision
        reads += viewModelScope.launch {
            try {
                val value = load()
                currentCoroutineContext().ensureActive()
                if (mine == revision) success(value)
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                if (mine == revision) failure()
            }
        }
    }
}
