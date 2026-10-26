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

    init { onAction(LeaderboardAction.ObserveContext) }

    override suspend fun handleActionInternal(action: LeaderboardAction) {
        when (action) {
            LeaderboardAction.ObserveContext -> observeContext()
            LeaderboardAction.Refresh -> if (ready) load()
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
        if (ready) load()
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
            if (request == revision) updateState { copy(loadFailed = true) }
        } finally {
            if (request == revision) {
                loadJob = null
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun accept(result: LeaderboardPage) {
        check(result.period == currentState.period && result.page == 1)
        updateState { copy(data = result.copy(items = result.items.take(LEADERBOARD_LIMIT)), loadFailed = false) }
    }

    private suspend fun openProfile(authorId: String) {
        val data = currentState.data ?: return
        val row = data.items.firstOrNull { it.authorId == authorId } ?: data.me?.takeIf { it.authorId == authorId }
        val slug = row?.authorSlug ?: return
        updateState { copy(expanded = false) }
        sendEvent(LeaderboardEvent(slug, row.username, row.avatarUrl, currentState.generation))
    }
}
