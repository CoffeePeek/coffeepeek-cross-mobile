package com.coffeepeek.feature.shelf.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class ShelfProfileVisibilityViewModel(private val repository: ShelfProfileRepository) :
    MviViewModel<ShelfProfileVisibilityState, ShelfProfileVisibilityAction, ShelfProfileVisibilityEvent>(ShelfProfileVisibilityState()) {
    private var owner: String? = null
    private var generation = 0
    private var work: Job? = null

    init { viewModelScope.launch { repository.owners.distinctUntilChanged().collect(::accountChanged) } }

    override suspend fun handleActionInternal(action: ShelfProfileVisibilityAction) {
        when (action) {
            ShelfProfileVisibilityAction.Refresh -> if (!currentState.loading && !currentState.busy) load()
            ShelfProfileVisibilityAction.SignIn -> if (owner == null) sendEvent(ShelfProfileVisibilityEvent.SignIn)
            is ShelfProfileVisibilityAction.SetVisible -> save(action.value)
        }
    }

    private fun accountChanged(value: String?) {
        generation++
        work?.cancel()
        owner = value
        updateState { ShelfProfileVisibilityState(signedIn = value != null) }
        if (value != null) load()
    }

    private fun load() {
        if (owner == null) return
        val epoch = generation
        updateState { copy(loading = true, loaded = false, visible = false, error = null) }
        launchWork(epoch, ShelfProfileVisibilityError.Load) {
            val result = repository.visibility().checked()
            if (epoch == generation) updateState { copy(visible = result.getOrDefault(false), loaded = result.isSuccess,
                error = if (result.isFailure) ShelfProfileVisibilityError.Load else null) }
        }
    }

    private fun save(value: Boolean) {
        if (owner == null || !currentState.loaded || currentState.loading || currentState.busy || value == currentState.visible) return
        val epoch = generation
        updateState { copy(busy = true, error = null) }
        launchWork(epoch, ShelfProfileVisibilityError.Save) {
            val result = repository.setVisibility(value).checked()
            if (epoch == generation) updateState { copy(visible = result.getOrNull() ?: visible,
                error = if (result.isFailure) ShelfProfileVisibilityError.Save else null) }
        }
    }

    private fun launchWork(epoch: Int, failure: ShelfProfileVisibilityError, block: suspend () -> Unit) {
        work = viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) {
                if (epoch == generation) updateState { copy(error = failure) }
                throw cancelled
            }
            catch (_: Exception) { if (epoch == generation) updateState { copy(error = failure) } }
            finally { if (epoch == generation) updateState { copy(loading = false, busy = false) } }
        }
    }

    private fun <T> Result<T>.checked(): Result<T> = also {
        exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }
}
