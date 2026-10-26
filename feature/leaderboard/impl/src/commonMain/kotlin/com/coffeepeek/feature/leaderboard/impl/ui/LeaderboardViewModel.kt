package com.coffeepeek.feature.leaderboard.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPage
import com.coffeepeek.feature.leaderboard.domain.model.LeaderboardPeriod
import com.coffeepeek.feature.leaderboard.domain.model.LEADERBOARD_LIMIT
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
    private val pages = mutableMapOf<LeaderboardPeriod, LeaderboardPage>()

    init { onAction(LeaderboardAction.ObserveContext) }

    override suspend fun handleActionInternal(action: LeaderboardAction) {
        when (action) {
            LeaderboardAction.ObserveContext -> observeContext()
            LeaderboardAction.Refresh -> if (ready && loadJob == null) {
                pages.clear()
                load()
            }
            LeaderboardAction.Retry -> if (ready && !currentState.isLoading) load()
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
            reset()
            if (ready) onAction(LeaderboardAction.Refresh)
        }
    }

    private fun cancelLoad() {
        revision++
        loadJob?.cancel()
        loadJob = null
    }

    private fun reset() {
        cancelLoad()
        pages.clear()
        updateState {
            LeaderboardState(period = period, isLoading = ready, generation = generation + 1)
        }
    }

    private suspend fun selectPeriod(period: LeaderboardPeriod) {
        if (period == currentState.period) return
        cancelLoad()
        val cached = pages[period]
        updateState {
            copy(period = period, data = cached ?: data, isLoading = false, loadFailed = false, generation = generation + 1)
        }
        if (ready && cached == null) load()
    }

    private suspend fun load() {
        loadJob?.cancel()
        val job = currentCoroutineContext().job
        val request = ++revision
        loadJob = job
        updateState { copy(isLoading = true, loadFailed = false) }
        requestPage(request)
    }

    private suspend fun requestPage(request: Long) {
        try {
            val result = repository.load(currentState.period).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (request == revision) accept(result)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (request == revision) updateState { copy(data = data?.takeIf { it.period == period }, loadFailed = true) }
        } finally {
            if (request == revision) {
                loadJob = null
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun accept(result: LeaderboardPage) {
        check(result.period == currentState.period && result.page == 1)
        val data = result.copy(items = result.items.take(LEADERBOARD_LIMIT))
        pages[data.period] = data
        updateState { copy(data = data, loadFailed = false) }
    }

    private suspend fun openProfile(authorId: String) {
        val data = currentState.data ?: return
        val row = data.items.firstOrNull { it.authorId == authorId } ?: data.me?.takeIf { it.authorId == authorId }
        val slug = row?.authorSlug ?: return
        updateState { copy(expanded = false) }
        sendEvent(LeaderboardEvent(slug, row.username, row.avatarUrl, currentState.generation))
    }
}
