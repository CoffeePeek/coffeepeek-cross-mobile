package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shop.domain.model.ShopViewer
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInVoteRepository
import com.coffeepeek.feature.shop.domain.repository.ShopDetailsRepository
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListEventEnvelope
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInListState
import com.coffeepeek.feature.shop.impl.ui.data.ratingSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job

internal class ShopCheckInListViewModel(
    private val shopId: String,
    private val detailsRepository: ShopDetailsRepository,
    private val voteRepository: ShopCheckInVoteRepository,
    private val currentViewer: suspend () -> Result<ShopViewer>,
    initialSessionGeneration: Int,
    initialRefreshGeneration: Long = 0,
) : MviViewModel<ShopCheckInListState, ShopCheckInListAction, ShopCheckInListEventEnvelope>(
    ShopCheckInListState(sessionGeneration = initialSessionGeneration),
) {
    private var sessionGeneration = initialSessionGeneration
    private var refreshGeneration = initialRefreshGeneration
    private var loadJob: Job? = null
    private var voteJob: Job? = null
    private var refreshQueued = false
    private var resumedOnce = false

    init { onAction(ShopCheckInListAction.Retry) }

    override suspend fun handleActionInternal(action: ShopCheckInListAction) {
        when (action) {
            ShopCheckInListAction.Retry -> load()
            ShopCheckInListAction.Resume -> if (resumedOnce) load() else { resumedOnce = true }
            is ShopCheckInListAction.Refresh -> {
                if (action.generation <= refreshGeneration) return
                refreshGeneration = action.generation
                if (loadJob != null || voteJob != null) refreshQueued = true else load()
            }
            is ShopCheckInListAction.SessionChanged -> {
                if (action.generation <= sessionGeneration) return
                val nextGeneration = action.generation
                sessionGeneration = nextGeneration
                loadJob?.cancel()
                voteJob?.cancel()
                loadJob = null
                voteJob = null
                refreshQueued = false
                updateState { ShopCheckInListState(sessionGeneration = nextGeneration) }
                load()
            }
            ShopCheckInListAction.Back -> emitEvent(ShopCheckInListEvent.Back)
            ShopCheckInListAction.SignIn -> emitEvent(ShopCheckInListEvent.SignIn)
            ShopCheckInListAction.Register -> emitEvent(ShopCheckInListEvent.Register)
            is ShopCheckInListAction.ToggleHelpful -> vote(action.checkInId)
            is ShopCheckInListAction.OpenAuthor -> {
                val row = currentState.checkIns.firstOrNull { it.id == action.checkInId } ?: return
                val slug = row.authorSlug?.takeIf(String::isNotBlank) ?: return
                emitEvent(ShopCheckInListEvent.OpenAuthor(slug, row.username, row.authorAvatarUrl))
            }
            is ShopCheckInListAction.Report -> {
                val checkIn = currentState.checkIns.firstOrNull { it.id == action.checkInId } ?: return
                if (currentState.owns(checkIn)) return
                if (currentState.isLoggedIn) emitEvent(ShopCheckInListEvent.Report(checkIn.id))
                else emitEvent(ShopCheckInListEvent.SignIn)
            }
            is ShopCheckInListAction.OpenPhoto -> {
                val checkIn = currentState.checkIns.firstOrNull { it.id == action.checkInId } ?: return
                if (action.index in checkIn.photoUrls.indices) {
                    emitEvent(ShopCheckInListEvent.OpenPhoto(checkIn.photoUrls, action.index))
                }
            }
        }
    }

    private suspend fun load() {
        // A GET must not overwrite a newer successful vote. Resume/retry is coalesced until it finishes.
        if (voteJob != null) { refreshQueued = true; return }
        if (loadJob != null) return
        val job = currentCoroutineContext().job
        val generation = sessionGeneration
        loadJob = job
        var wasCancelled = false
        updateState { copy(isLoading = true, hasLoadError = false) }
        try {
            val viewer = currentViewer().getOrThrow()
            currentCoroutineContext().ensureActive()
            if (generation != sessionGeneration || loadJob !== job) return
            val details = detailsRepository.getDetails(shopId).getOrThrow()
            if (generation != sessionGeneration || loadJob !== job) return
            val visible = if (viewer.isLoggedIn) details.checkIns else details.checkIns.take(1)
            updateState {
                copy(
                    shopTitle = details.overview.title,
                    overallRating = details.overview.rating,
                    checkInCount = maxOf(details.overview.reviewCount, details.checkIns.size),
                    ratingSampleSize = details.checkIns.size,
                    ratingSummary = details.checkIns.ratingSummary(),
                    checkIns = visible,
                    hiddenCheckInCount = details.checkIns.size - visible.size,
                    ownCheckInIds = if (viewer.isLoggedIn) details.userCheckIns.map { it.id }.toSet() else emptySet(),
                    isLoggedIn = viewer.isLoggedIn,
                    currentUserId = viewer.userId.takeIf { viewer.isLoggedIn },
                    hasLoaded = true,
                    hasLoadError = false,
                    hasVoteError = false,
                )
            }
        } catch (cancelled: CancellationException) {
            wasCancelled = true
            throw cancelled
        } catch (_: Exception) {
            if (generation == sessionGeneration && loadJob === job) updateState { copy(hasLoadError = true) }
        } finally {
            if (loadJob === job) {
                loadJob = null
                updateState { copy(isLoading = false) }
                refreshIfQueued(generation, wasCancelled)
            }
        }
    }

    private suspend fun vote(checkInId: String) {
        val checkIn = currentState.checkIns.firstOrNull { it.id == checkInId } ?: return
        if (currentState.owns(checkIn)) return
        if (!currentState.isLoggedIn) { emitEvent(ShopCheckInListEvent.SignIn); return }
        if (voteJob != null || loadJob != null) return
        val job = currentCoroutineContext().job
        val generation = sessionGeneration
        voteJob = job
        var wasCancelled = false
        updateState { copy(pendingVoteId = checkInId, hasVoteError = false) }
        try {
            val vote = voteRepository.setHelpful(checkInId, !checkIn.isHelpfulByCurrentUser).getOrThrow()
            if (generation != sessionGeneration || voteJob !== job) return
            updateState {
                copy(checkIns = checkIns.map { item ->
                    if (item.id == checkInId) item.copy(helpfulCount = vote.helpfulCount,
                        isHelpfulByCurrentUser = vote.isHelpful) else item
                })
            }
        } catch (cancelled: CancellationException) {
            wasCancelled = true
            throw cancelled
        } catch (_: Exception) {
            if (generation == sessionGeneration && voteJob === job) updateState { copy(hasVoteError = true) }
        } finally {
            if (voteJob === job) {
                voteJob = null
                updateState { copy(pendingVoteId = null) }
                refreshIfQueued(generation, wasCancelled)
            }
        }
    }

    private suspend fun emitEvent(event: ShopCheckInListEvent) {
        sendEvent(ShopCheckInListEventEnvelope(event, sessionGeneration))
    }

    private suspend fun refreshIfQueued(generation: Int, wasCancelled: Boolean) {
        if (!refreshQueued) return
        refreshQueued = false
        if (!wasCancelled && currentCoroutineContext().job.isActive && generation == sessionGeneration) {
            onAction(ShopCheckInListAction.Retry)
        }
    }
}
