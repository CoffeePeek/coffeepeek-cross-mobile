package com.coffeepeek.feature.homecheckin.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationFailure
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job

/**
 * One preparation and one account lease. Composition must also guard the repository;
 * this predicate prevents stale UI work, not server-side access after account replacement.
 */
internal class HomeCheckInDetailViewModel(
    private val preparationId: String,
    private val repository: HomePreparationRepository,
    private val sessionGeneration: Int,
    private val canAccess: () -> Boolean,
) : MviViewModel<HomeCheckInDetailState, HomeCheckInDetailAction, HomeCheckInDetailEventEnvelope>(
    HomeCheckInDetailState(),
) {
    private var loadJob: Job? = null
    private var deleteJob: Job? = null
    private var revision = 0L

    init { onAction(HomeCheckInDetailAction.Reload) }

    override suspend fun handleActionInternal(action: HomeCheckInDetailAction) {
        if (action == HomeCheckInDetailAction.RevokeAccess || !canAccess()) {
            revokeAccess()
            return
        }
        if (currentState.isClosed || currentState.isAccessRevoked) return
        when (action) {
            HomeCheckInDetailAction.Reload -> load()
            HomeCheckInDetailAction.Close -> if (deleteJob == null) {
                invalidatePendingWork()
                updateState { HomeCheckInDetailState(isClosed = true) }
                emit(HomeCheckInDetailEvent.Close)
            }
            HomeCheckInDetailAction.Edit -> if (canUseEntry()) {
                emit(HomeCheckInDetailEvent.Edit(preparationId))
            }
            is HomeCheckInDetailAction.OpenPhoto -> if (canUseEntry()) {
                val photos = currentState.entry!!.photos
                if (action.index in photos.indices && photos.all { it.url.isNotBlank() }) {
                    emit(HomeCheckInDetailEvent.OpenPhoto(photos.map { it.detailUrl ?: it.url }, action.index,
                        photos.map { it.url }))
                }
            }
            HomeCheckInDetailAction.RequestDelete -> if (canUseEntry()) {
                revision++
                updateState { copy(showDeleteConfirmation = true, deleteIssue = null) }
            }
            HomeCheckInDetailAction.CancelDelete -> if (deleteJob == null) {
                updateState { copy(showDeleteConfirmation = false) }
            }
            HomeCheckInDetailAction.ConfirmDelete -> delete()
            HomeCheckInDetailAction.RevokeAccess -> Unit
        }
    }

    /** A buffered event is valid only for its account and still-current detail operation. */
    fun canDeliver(envelope: HomeCheckInDetailEventEnvelope): Boolean =
        canAccess() && !currentState.isAccessRevoked &&
            envelope.sessionGeneration == sessionGeneration && envelope.revision == revision

    private fun canUseEntry() = currentState.entry != null && loadJob == null &&
        deleteJob == null && !currentState.showDeleteConfirmation

    private suspend fun load() {
        if (loadJob != null || deleteJob != null || currentState.showDeleteConfirmation) return
        if (preparationId.isBlank()) {
            updateState { copy(loadIssue = HomePreparationIssue.InvalidInput) }
            return
        }
        val job = currentCoroutineContext().job
        val expectedRevision = ++revision
        loadJob = job
        updateState { copy(isLoading = true, loadIssue = null, deleteIssue = null) }
        try {
            val entry = repository.detail(preparationId).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (!canAccess()) { revokeAccess(); return }
            if (loadJob !== job || revision != expectedRevision) return
            if (entry.id != preparationId) throw HomePreparationFailure(HomePreparationIssue.InvalidResponse)
            updateState { copy(entry = entry, loadIssue = null) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (!canAccess()) { revokeAccess(); return }
            if (loadJob === job && revision == expectedRevision && job.isActive) {
                val issue = failure.issue()
                updateState { copy(loadIssue = issue, entry = entry.takeUnless { issue.invalidatesEntry() }) }
            }
        } finally {
            if (loadJob === job) {
                loadJob = null
                updateState { copy(isLoading = false) }
            }
        }
    }

    private suspend fun delete() {
        if (!currentState.showDeleteConfirmation || currentState.entry == null ||
            loadJob != null || deleteJob != null) return
        val job = currentCoroutineContext().job
        val expectedRevision = ++revision
        deleteJob = job
        updateState { copy(isDeleting = true, showDeleteConfirmation = false, deleteIssue = null) }
        try {
            repository.delete(preparationId).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (!canAccess()) { revokeAccess(); return }
            if (deleteJob !== job || revision != expectedRevision) return
            // Invalidate already-buffered photo/edit effects before delivering the single outcome.
            revision++
            updateState { HomeCheckInDetailState(isClosed = true) }
            emit(HomeCheckInDetailEvent.Deleted(preparationId))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (!canAccess()) { revokeAccess(); return }
            if (deleteJob === job && revision == expectedRevision && job.isActive) {
                val issue = failure.issue()
                updateState { copy(deleteIssue = issue, entry = entry.takeUnless { issue.invalidatesEntry() }) }
            }
        } finally {
            if (deleteJob === job) {
                deleteJob = null
                updateState { copy(isDeleting = false) }
            }
        }
    }

    private suspend fun emit(event: HomeCheckInDetailEvent) {
        sendEvent(HomeCheckInDetailEventEnvelope(event, sessionGeneration, revision))
    }

    private fun invalidatePendingWork() {
        revision++
        loadJob?.cancel()
        deleteJob?.cancel()
        loadJob = null
        deleteJob = null
    }

    private fun revokeAccess() {
        invalidatePendingWork()
        updateState { HomeCheckInDetailState(isAccessRevoked = true) }
    }

    override fun onCleared() {
        revokeAccess()
        super.onCleared()
    }
}

private fun Exception.issue() = (this as? HomePreparationFailure)?.issue ?: HomePreparationIssue.RequestFailed

private fun HomePreparationIssue.invalidatesEntry() = this in setOf(
    HomePreparationIssue.Unauthorized, HomePreparationIssue.Forbidden, HomePreparationIssue.NotFound,
)
