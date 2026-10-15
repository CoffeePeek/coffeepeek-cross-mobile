package com.coffeepeek.feature.favorites.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesEvent
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

internal class FavoritesViewModel(
    private val repository: FavoritesRepository,
    private val roasterRepository: FavoriteRoastersRepository? = null,
) : MviViewModel<FavoritesUiState, FavoritesAction, FavoritesEvent>(
    FavoritesUiState(areRoastersLoading = roasterRepository != null),
) {
    private var observation: Job? = null
    private var roasterObservation: Job? = null
    private var generation = 0

    init { retry() }

    override suspend fun handleActionInternal(action: FavoritesAction) {
        when (action) {
            FavoritesAction.Retry -> retry()
            is FavoritesAction.Remove -> remove(action.shopId)
            is FavoritesAction.OpenShop -> sendEvent(FavoritesEvent.OpenShop(action.shopId))
            is FavoritesAction.RemoveRoaster -> removeRoaster(action.roasterId)
            is FavoritesAction.OpenRoaster -> currentState.roasters.firstOrNull { it.id == action.roasterId }
                ?.publicSlug?.takeIf(String::isNotBlank)?.let { sendEvent(FavoritesEvent.OpenRoaster(it)) }
            FavoritesAction.Back -> sendEvent(FavoritesEvent.Back)
        }
    }

    private fun retry() {
        val current = ++generation
        observation?.cancel()
        roasterObservation?.cancel()
        updateState { copy(isLoading = shops.isEmpty(), loadFailed = false, actionFailed = false,
            areRoastersLoading = roasterRepository != null && roasters.isEmpty(), roastersLoadFailed = false) }
        observation = viewModelScope.launch {
            repository.observe().catch { error ->
                if (error is CancellationException) throw error
                emit(Result.failure(error))
            }.collect { result ->
                result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                if (current == generation) updateState {
                    copy(shops = result.getOrNull() ?: shops, isLoading = false, loadFailed = result.isFailure)
                }
            }
        }
        roasterObservation = roasterRepository?.let { source -> viewModelScope.launch {
            source.observe().catch { error ->
                if (error is CancellationException) throw error
                emit(Result.failure(error))
            }.collect { result ->
                result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                if (current == generation) updateState {
                    copy(roasters = result.getOrNull() ?: roasters, areRoastersLoading = false,
                        roastersLoadFailed = result.isFailure)
                }
            }
        } }
    }

    private suspend fun removeRoaster(id: String) {
        val source = roasterRepository ?: return
        if (id in currentState.removingRoasters || currentState.roasters.none { it.id == id }) return
        updateState { copy(removingRoasters = removingRoasters + id, actionFailed = false) }
        try {
            val result = source.remove(id)
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            updateState { copy(actionFailed = result.isFailure) }
        } finally {
            updateState { copy(removingRoasters = removingRoasters - id) }
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
