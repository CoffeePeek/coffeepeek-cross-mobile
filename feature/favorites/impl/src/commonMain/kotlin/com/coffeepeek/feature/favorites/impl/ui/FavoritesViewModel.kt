package com.coffeepeek.feature.favorites.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesEvent
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class FavoritesViewModel(private val repository: FavoritesRepository) :
    MviViewModel<FavoritesUiState, FavoritesAction, FavoritesEvent>(FavoritesUiState()) {
    private var observation: Job? = null
    private var generation = 0

    init { retry() }

    override suspend fun handleActionInternal(action: FavoritesAction) {
        when (action) {
            FavoritesAction.Retry -> retry()
            is FavoritesAction.Remove -> remove(action.shopId)
            is FavoritesAction.OpenShop -> sendEvent(FavoritesEvent.OpenShop(action.shopId))
            FavoritesAction.Back -> sendEvent(FavoritesEvent.Back)
        }
    }

    private fun retry() {
        val current = ++generation
        observation?.cancel()
        updateState { copy(isLoading = shops.isEmpty(), loadFailed = false, actionFailed = false) }
        observation = viewModelScope.launch {
            repository.observe().collect { result ->
                result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                if (current == generation) updateState {
                    copy(shops = result.getOrNull() ?: shops, isLoading = false, loadFailed = result.isFailure)
                }
            }
        }
    }

    private suspend fun remove(shopId: String) {
        if (shopId in currentState.removing || currentState.shops.none { it.id == shopId }) return
        updateState { copy(removing = removing + shopId, actionFailed = false) }
        try {
            val result = repository.remove(shopId)
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            // Observation owns the list. Failed writes never optimistically erase cards.
            updateState { copy(actionFailed = result.isFailure) }
        } finally {
            updateState { copy(removing = removing - shopId) }
        }
    }
}
