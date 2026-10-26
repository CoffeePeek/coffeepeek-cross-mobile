package com.coffeepeek.feature.leaderboard.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod
import com.coffeepeek.feature.leaderboard.domain.repository.LeaderboardRepository
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardAction
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardEvent
import com.coffeepeek.feature.leaderboard.impl.ui.compose.model.LeaderboardState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.job

internal class LeaderboardViewModel(
    private val repository: LeaderboardRepository,
    private val contextChanges: Flow<Boolean>,
) : MviViewModel<LeaderboardState, LeaderboardAction, LeaderboardEvent>(LeaderboardState()) {
    private var observing = false
    private var ready = false
    private var loadJob: Job? = null
    private var revision = 0L

    init { onAction(LeaderboardAction.ObserveContext) }

    override suspend fun handleActionInternal(action: LeaderboardAction) {
        when (action) {
            LeaderboardAction.ObserveContext -> observeContext()
            LeaderboardAction.Refresh -> if (ready) load(1)
            LeaderboardAction.Retry -> if (ready && !currentState.isLoading) load(currentState.failedPage ?: 1)
            LeaderboardAction.LoadMore -> loadMore()
            is LeaderboardAction.SelectPeriod -> selectPeriod(action.period)
            is LeaderboardAction.SetExpanded -> updateState { copy(expanded = action.expanded) }
            is LeaderboardAction.OpenProfile -> openProfile(action.authorId)
        }
    }

    private suspend fun observeContext() {
        if (observing) return
        observing = true
        contextChanges.collect { available ->
            ready = available
            reset(currentState.period, closeSheet = true)
            if (ready) onAction(LeaderboardAction.Refresh)
        }
    }

    private fun reset(period: LeaderboardPeriod, closeSheet: Boolean) {
        revision++
        loadJob?.cancel()
        loadJob = null
        updateState {
            LeaderboardState(period = period, expanded = expanded && !closeSheet, generation = generation + 1)
        }
    }

    private suspend fun selectPeriod(period: LeaderboardPeriod) {
        if (period == currentState.period) return
        reset(period, closeSheet = false)
        if (ready) load(1)
    }

    private suspend fun loadMore() {
        val data = currentState.data ?: return
        if (!ready || currentState.isLoading || currentState.failedPage != null || !data.hasMore) return
        load(data.page + 1)
    }

    private suspend fun load(page: Int) {
        loadJob?.cancel()
        val job = currentCoroutineContext().job
        val request = ++revision
        loadJob = job
        val previous = currentState.data
        updateState { copy(isLoading = true, failedPage = null) }
        requestPage(page, previous, request)
    }

    private suspend fun requestPage(page: Int, previous: LeaderboardPage?, request: Long) {
        try {
            val result = repository.load(currentState.period, page, previous?.startsOn?.takeIf { page > 1 }).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (request == revision) accept(result, previous, page)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (request == revision) updateState { copy(failedPage = page) }
        } finally {
            if (request == revision) {
                loadJob = null
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun accept(result: LeaderboardPage, previous: LeaderboardPage?, page: Int) {
        check(result.period == currentState.period && result.page == page)
        if (page > 1) {
            check(previous != null && result.startsOn == previous.startsOn &&
                result.endsOnExclusive == previous.endsOnExclusive && result.timeZone == previous.timeZone)
        }
        val items = if (page == 1) result.items else (previous!!.items + result.items).distinctBy { it.authorId }
        updateState { copy(data = result.copy(items = items), failedPage = null) }
    }

    private suspend fun openProfile(authorId: String) {
        val data = currentState.data ?: return
        val row = data.items.firstOrNull { it.authorId == authorId } ?: data.me?.takeIf { it.authorId == authorId }
        val slug = row?.authorSlug ?: return
        updateState { copy(expanded = false) }
        sendEvent(LeaderboardEvent(slug, row.username, row.avatarUrl, currentState.generation))
    }
}
