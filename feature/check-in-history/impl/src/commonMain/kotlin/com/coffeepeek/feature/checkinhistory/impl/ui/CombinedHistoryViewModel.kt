package com.coffeepeek.feature.checkinhistory.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.HomeHistoryReader
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryAction
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryLoadState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryPageState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.CombinedHistoryState
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryKindFilter
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistorySource
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistoryTimelineItem
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

/** Combined read coordination only; the active legacy screen is not switched by preparing this VM. */
internal class CombinedHistoryViewModel private constructor(
    private val shopReader: CheckInHistoryRepository,
    private val homeReader: HomeHistoryReader,
    private val calendarProvider: HistoryCalendarProvider,
    loggedIn: Boolean,
    generation: Int,
    initialContentGeneration: Int,
    private var context: HistoryCalendarContext,
) : MviViewModel<CombinedHistoryState, CombinedHistoryAction, Nothing>(initialState(context, loggedIn, generation)) {
    constructor(shopReader: CheckInHistoryRepository, homeReader: HomeHistoryReader,
        calendarProvider: HistoryCalendarProvider, loggedIn: Boolean, generation: Int = 0,
        initialContentGeneration: Int = 0) : this(shopReader, homeReader, calendarProvider, loggedIn,
            generation, initialContentGeneration, calendarProvider.snapshot())

    // The same verified request lifetime is composed for four independent reads, not put in core/base.
    private class ReadSlot {
        var job: Job? = null
        var revision = 0L
        var stale = true
        var queued = false
        fun cancel() {
            val previous = job
            job = null
            revision++
            stale = true
            queued = false
            previous?.cancel()
        }
    }

    private val shopCalendar = ReadSlot()
    private val homeCalendar = ReadSlot()
    private val shopPage = ReadSlot()
    private val homePage = ReadSlot()
    private val slots = listOf(shopCalendar, homeCalendar, shopPage, homePage)
    private var firstResume = true
    private var contentGeneration = initialContentGeneration
    private var contextRevision = 0L

    init { require(initialContentGeneration >= 0); loadVisible() }

    override suspend fun handleActionInternal(action: CombinedHistoryAction) {
        when (action) {
            is CombinedHistoryAction.SelectFilter -> if (action.filter != currentState.filter) {
                updateState { copy(filter = action.filter) }
                suspendInvisibleReads()
                loadVisible()
            }
            is CombinedHistoryAction.SelectDate -> {
                val date = action.date
                if (date == null || currentState.month.containsDate(date)) {
                    select(currentState.month, date.takeUnless { it == currentState.selectedDate })
                }
            }
            is CombinedHistoryAction.ChangeMonth -> {
                if (action.offset == 0) return
                val month = try { currentState.month.plusMonths(action.offset) }
                    catch (_: IllegalArgumentException) { return }
                if (month.date(1).take(7) <= currentState.today.take(7)) select(month, null)
            }
            CombinedHistoryAction.Today -> if (synchronizeContext()) {
                select(requireNotNull(HistoryMonth.fromDate(currentState.today)), currentState.today)
                loadVisible()
            }
            CombinedHistoryAction.Refresh -> refresh()
            CombinedHistoryAction.Resume -> {
                val previous = contextRevision
                val available = synchronizeContext()
                if (firstResume) {
                    firstResume = false
                    if (available && previous != contextRevision) loadVisible()
                } else if (available) invalidateAndLoad()
            }
            is CombinedHistoryAction.ContentChanged -> if (action.generation > contentGeneration) {
                contentGeneration = action.generation
                refresh()
            }
            is CombinedHistoryAction.SessionChanged -> replaceSession(action)
            CombinedHistoryAction.LoadMore -> {
                loadMore(HistorySource.Shop, retry = false)
                loadMore(HistorySource.Home, retry = false)
            }
            is CombinedHistoryAction.RetryLoadMore -> loadMore(action.source, retry = true)
            is CombinedHistoryAction.RetryCalendar -> if (synchronizeContext()) retryCalendar(action.source)
            is CombinedHistoryAction.RetryTimeline -> if (synchronizeContext()) {
                if (action.source == HistorySource.Shop && currentState.selectedDate != null) retryCalendar(action.source)
                else if (action.source == HistorySource.Shop) loadShopPage(reset = true) else loadHomePage(reset = true)
            }
        }
    }

    private fun select(month: HistoryMonth, date: String?) {
        val previous = currentState
        if (previous.month == month && previous.selectedDate == date) return
        if (previous.month != month) {
            shopCalendar.cancel(); homeCalendar.cancel()
            updateState { copy(shopCalendar = CombinedHistoryLoadState(emptyMap()), homeCalendar = CombinedHistoryLoadState(emptyMap())) }
        }
        if (previous.selectedDate != date) {
            homePage.cancel()
            updateState { copy(homes = CombinedHistoryPageState()) }
        }
        updateState { copy(month = month, selectedDate = date) }
        suspendInvisibleReads()
        loadVisible()
    }

    private fun suspendInvisibleReads() {
        if (!includes(HistorySource.Shop)) {
            if (shopCalendar.job != null) shopCalendar.cancel()
            updateState { copy(shopCalendar = shopCalendar.copy(isLoading = false)) }
        }
        if (!includes(HistorySource.Home)) {
            if (homeCalendar.job != null) homeCalendar.cancel()
            if (homePage.job != null) homePage.cancel()
            updateState { copy(homeCalendar = homeCalendar.copy(isLoading = false), homes = homes.copy(isLoading = false, isLoadingMore = false)) }
        }
        if (!includes(HistorySource.Shop) || currentState.selectedDate != null) {
            if (shopPage.job != null) shopPage.cancel()
            updateState { copy(shops = shops.copy(isLoading = false, isLoadingMore = false)) }
        }
    }

    private fun replaceSession(action: CombinedHistoryAction.SessionChanged) {
        if (action.generation <= currentState.sessionGeneration) return
        cancelAll()
        firstResume = true
        val filter = currentState.filter
        updateState { initialState(context, action.isLoggedIn, action.generation).copy(filter = filter) }
        synchronizeContext()
        loadVisible()
    }

    private fun synchronizeContext(): Boolean = try {
        val next = calendarProvider.snapshot()
        val latest = requireNotNull(HistoryMonth.fromDate(next.today))
        val changed = currentState.isCalendarUnavailable || next.today != context.today || next.zoneId != context.zoneId
        val zoneChanged = next.zoneId != context.zoneId
        context = next
        if (changed) {
            contextRevision++
            cancelAll()
            val clamped = currentState.month.date(1) > latest.date(1)
            val clearDate = zoneChanged || clamped
            updateState { copy(today = next.today, isCalendarUnavailable = false,
                month = if (clamped) latest else month, selectedDate = if (clearDate) null else selectedDate,
                shopCalendar = if (zoneChanged || clamped) CombinedHistoryLoadState(emptyMap()) else shopCalendar,
                homeCalendar = if (clamped) CombinedHistoryLoadState(emptyMap()) else homeCalendar,
                shops = if (zoneChanged) CombinedHistoryPageState() else shops,
                homes = if (clearDate && selectedDate != null) CombinedHistoryPageState() else homes) }
        }
        true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        cancelAll()
        updateState { copy(isCalendarUnavailable = true) }
        false
    }

    private fun refresh() { if (synchronizeContext()) invalidateAndLoad() }

    private fun invalidateAndLoad() {
        if (currentState.shops.isLoadingMore) {
            shopPage.cancel()
            updateState { copy(shops = shops.copy(isLoadingMore = false)) }
        }
        if (currentState.homes.isLoadingMore) {
            homePage.cancel()
            updateState { copy(homes = homes.copy(isLoadingMore = false)) }
        }
        slots.forEach { it.revision++; it.stale = true; if (it.job != null) it.queued = true }
        loadVisible(retryErrors = true)
    }

    private fun cancelAll() {
        slots.forEach(ReadSlot::cancel)
        updateState { copy(shopCalendar = shopCalendar.copy(isLoading = false), homeCalendar = homeCalendar.copy(isLoading = false),
            shops = shops.copy(isLoading = false, isLoadingMore = false), homes = homes.copy(isLoading = false, isLoadingMore = false)) }
    }

    private fun includes(source: HistorySource) = currentState.filter.includes(source)

    private fun loadVisible(retryErrors: Boolean = false) {
        if (!currentState.isLoggedIn || currentState.isCalendarUnavailable) return
        if (includes(HistorySource.Shop)) {
            if (shopCalendar.stale && (retryErrors || !currentState.shopCalendar.hasError)) loadShopCalendar()
            if (currentState.selectedDate == null && shopPage.stale && (retryErrors || !currentState.shops.hasError)) loadShopPage(true)
        }
        if (includes(HistorySource.Home)) {
            if (homeCalendar.stale && (retryErrors || !currentState.homeCalendar.hasError)) loadHomeCalendar()
            if (homePage.stale && (retryErrors || !currentState.homes.hasError)) loadHomePage(true)
        }
    }

    private fun retryCalendar(source: HistorySource) {
        if (source == HistorySource.Shop) loadShopCalendar() else loadHomeCalendar()
    }

    private fun loadShopCalendar() {
        if (!includes(HistorySource.Shop)) return
        val month = currentState.month
        val snapshot = context
        read(shopCalendar,
            start = { updateState { copy(shopCalendar = shopCalendar.copy(isLoading = true, hasError = false)) } },
            load = {
                shopReader.period(snapshot.period(month)).getOrThrow().distinctBy { it.id }.map { entry ->
                    val date = requireNotNull(snapshot.localDate(entry.effectiveVisitedAt))
                    requireNotNull(HistoryMonth.fromDate(date))
                    HistoryTimelineItem.Shop(entry, date)
                }.filter { month.containsDate(it.date) }.groupBy { it.date }
            },
            success = { rows -> updateState { copy(shopCalendar = shopCalendar.copy(content = rows, hasLoaded = true)) } },
            failure = { updateState { copy(shopCalendar = shopCalendar.copy(hasError = true)) } },
            finish = { updateState { copy(shopCalendar = shopCalendar.copy(isLoading = false)) } },
            followUp = ::loadShopCalendar)
    }

    private fun loadHomeCalendar() {
        if (!includes(HistorySource.Home)) return
        val month = currentState.month
        read(homeCalendar,
            start = { updateState { copy(homeCalendar = homeCalendar.copy(isLoading = true, hasError = false)) } },
            load = {
                val days = homeReader.calendar(HomeHistoryDateRange.month(month)).getOrThrow()
                check(days.distinctBy { it.preparedOn }.size == days.size) { "Duplicate Home calendar dates" }
                days.filter { month.containsDate(it.preparedOn) }.associate { it.preparedOn to it.brewCount }
            },
            success = { days -> updateState { copy(homeCalendar = homeCalendar.copy(content = days, hasLoaded = true)) } },
            failure = { updateState { copy(homeCalendar = homeCalendar.copy(hasError = true)) } },
            finish = { updateState { copy(homeCalendar = homeCalendar.copy(isLoading = false)) } },
            followUp = ::loadHomeCalendar)
    }

    private fun loadMore(source: HistorySource, retry: Boolean) {
        if (!includes(source)) return
        if (source == HistorySource.Shop) {
            val state = currentState.shops
            if (currentState.selectedDate != null || shopPage.stale || !state.hasLoaded || !state.hasMore ||
                state.hasError || state.hasLoadMoreError && !retry) return
            loadShopPage(false)
        } else {
            val state = currentState.homes
            if (homePage.stale || !state.hasLoaded || !state.hasMore || state.hasError || state.hasLoadMoreError && !retry) return
            loadHomePage(false)
        }
    }

    private fun loadShopPage(reset: Boolean) {
        if (!includes(HistorySource.Shop) || currentState.selectedDate != null) return
        val number = if (reset) 1 else currentState.shops.page + 1
        val snapshot = context
        read(shopPage,
            start = { updateState { copy(shops = shops.copy(isLoading = reset, isLoadingMore = !reset, hasError = false, hasLoadMoreError = false)) } },
            load = {
                val page = shopReader.page(number, PAGE_SIZE).getOrThrow()
                check(page.page == number && page.pageSize == PAGE_SIZE && (page.items.isNotEmpty() || !page.hasMore))
                val rows = page.items.map { entry ->
                    val date = requireNotNull(snapshot.localDate(entry.effectiveVisitedAt))
                    requireNotNull(HistoryMonth.fromDate(date))
                    HistoryTimelineItem.Shop(entry, date)
                }
                rows to page.hasMore
            },
            success = { (rows, more) -> updateState { copy(shops = shops.copy(
                items = (if (reset) rows else shops.items + rows).distinctBy { it.key }, page = number, hasMore = more, hasLoaded = true)) } },
            failure = { updateState { copy(shops = shops.copy(hasError = reset, hasLoadMoreError = !reset)) } },
            finish = { updateState { copy(shops = shops.copy(isLoading = false, isLoadingMore = false)) } },
            followUp = { loadShopPage(true) })
    }

    private fun loadHomePage(reset: Boolean) {
        if (!includes(HistorySource.Home)) return
        val number = if (reset) 1 else currentState.homes.page + 1
        val selected = currentState.selectedDate
        read(homePage,
            start = { updateState { copy(homes = homes.copy(isLoading = reset, isLoadingMore = !reset, hasError = false, hasLoadMoreError = false)) } },
            load = {
                val range = selected?.let { HomeHistoryDateRange(it, it) }
                val page = homeReader.page(number, PAGE_SIZE, range).getOrThrow()
                check(page.page == number && page.pageSize == PAGE_SIZE && (page.items.isNotEmpty() || !page.hasMore))
                val rows = page.items.map { entry ->
                    requireNotNull(HistoryMonth.fromDate(entry.preparedOn))
                    check(selected == null || selected == entry.preparedOn) { "Home page does not match selected date" }
                    HistoryTimelineItem.Home(entry)
                }
                rows to page.hasMore
            },
            success = { (rows, more) -> updateState { copy(homes = homes.copy(
                items = (if (reset) rows else homes.items + rows).distinctBy { it.key }, page = number, hasMore = more, hasLoaded = true)) } },
            failure = { updateState { copy(homes = homes.copy(hasError = reset, hasLoadMoreError = !reset)) } },
            finish = { updateState { copy(homes = homes.copy(isLoading = false, isLoadingMore = false)) } },
            followUp = { loadHomePage(true) })
    }

    private fun <T> read(slot: ReadSlot, start: () -> Unit, load: suspend () -> T,
        success: (T) -> Unit, failure: () -> Unit, finish: () -> Unit, followUp: () -> Unit) {
        if (slot.job != null || !currentState.isLoggedIn || currentState.isCalendarUnavailable) return
        val generation = currentState.sessionGeneration
        val revision = slot.revision
        start()
        val work = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val job = currentCoroutineContext().job
            var cancelled = false
            try {
                val result = load()
                currentCoroutineContext().ensureActive()
                if (slot.job === job && revision == slot.revision && generation == currentState.sessionGeneration) {
                    slot.stale = false
                    success(result)
                }
            } catch (cancellation: CancellationException) {
                cancelled = true
                throw cancellation
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                if (slot.job === job && revision == slot.revision && generation == currentState.sessionGeneration) failure()
            } finally {
                if (slot.job === job) {
                    slot.job = null
                    finish()
                    val queued = slot.queued
                    slot.queued = false
                    if (queued && !cancelled && currentCoroutineContext().isActive) followUp()
                }
            }
        }
        slot.job = work
        work.start()
    }

    companion object {
        private const val PAGE_SIZE = 20
        private fun initialState(context: HistoryCalendarContext, loggedIn: Boolean, generation: Int): CombinedHistoryState {
            require(generation >= 0)
            return CombinedHistoryState(context.today, requireNotNull(HistoryMonth.fromDate(context.today)), loggedIn, generation)
        }
    }
}
