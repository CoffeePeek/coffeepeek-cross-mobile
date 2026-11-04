package com.coffeepeek.feature.favorites.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.favorites.domain.repository.FavoritesRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteRoastersRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoriteDrinksRepository
import com.coffeepeek.feature.favorites.domain.repository.FavoritesAccount
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesAction
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesEvent
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesUiState
import com.coffeepeek.feature.favorites.impl.ui.compose.model.withSettledInitialLoad
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class FavoritesViewModel(
    private val repository: FavoritesRepository,
    private val roasterRepository: FavoriteRoastersRepository? = null,
    private val drinkRepository: FavoriteDrinksRepository? = null,
    private val account: FavoritesAccount? = null,
) : MviViewModel<FavoritesUiState, FavoritesAction, FavoritesEvent>(
    FavoritesUiState(areRoastersLoading = roasterRepository != null, areDrinksLoading = drinkRepository != null),
) {
    private var observation: Job? = null
    private var roasterObservation: Job? = null
    private var drinkObservation: Job? = null
    private var generation = 0
    private var ownerGeneration = 0
    private var observedOwner: String? = null
    private var ownerInitialized = account == null

    init { if (account == null) retry() else observeAccount(account) }

    override suspend fun handleActionInternal(action: FavoritesAction) {
        when (action) {
            FavoritesAction.Retry -> retry()
            is FavoritesAction.Remove -> remove(action.shopId)
            is FavoritesAction.OpenShop -> currentState.shops.firstOrNull { it.id == action.shopId }
                ?.publicSlug?.takeIf(String::isNotBlank)?.let { sendEvent(FavoritesEvent.OpenShop(it)) }
            is FavoritesAction.RemoveRoaster -> removeRoaster(action.roasterId)
            is FavoritesAction.RemoveDrink -> removeDrink(action.id)
            is FavoritesAction.OpenRoaster -> currentState.roasters.firstOrNull { it.id == action.roasterId }
                ?.publicSlug?.takeIf(String::isNotBlank)?.let { sendEvent(FavoritesEvent.OpenRoaster(it)) }
            FavoritesAction.Back -> sendEvent(FavoritesEvent.Back)
            FavoritesAction.Collections -> sendEvent(FavoritesEvent.OpenCollections)
            FavoritesAction.Share -> if (hasCurrentOwner() && (currentState.shops.isNotEmpty() || currentState.roasters.isNotEmpty())) sendEvent(FavoritesEvent.Share)
        }
    }

    private fun retry() {
        if (!hasCurrentOwner()) return
        val current = ++generation
        stopObservations()
        updateState { copy(isLoading = shops.isEmpty(), loadFailed = false, actionFailed = false,
            areRoastersLoading = roasterRepository != null && roasters.isEmpty(), roastersLoadFailed = false,
            areDrinksLoading = drinkRepository != null && drinks.isEmpty(), drinksLoadFailed = false) }
        observation = observeShops(current, observedOwner)
        roasterObservation = roasterRepository?.let { observeRoasters(it, current, observedOwner) }
        drinkObservation = drinkRepository?.let { observeDrinks(it, current, observedOwner) }
    }

    private fun observeAccount(source: FavoritesAccount) {
        viewModelScope.launch {
            source.identities.distinctUntilChanged().collect { owner ->
                if (owner == source.currentId()) activateOwner(owner)
            }
        }
    }

    private fun activateOwner(owner: String?) {
        observedOwner = owner
        ownerInitialized = true
        ownerGeneration++
        generation++
        stopObservations()
        updateState { FavoritesUiState(isLoading = owner != null,
            areRoastersLoading = owner != null && roasterRepository != null,
            areDrinksLoading = owner != null && drinkRepository != null, hasSettledInitialLoad = owner == null) }
        if (owner != null) retry()
    }

    private fun stopObservations() {
        observation?.cancel()
        roasterObservation?.cancel()
        drinkObservation?.cancel()
    }

    private fun hasCurrentOwner(): Boolean = account == null ||
        ownerInitialized && observedOwner != null && account.currentId() == observedOwner

    private fun accepts(current: Int, owner: String?): Boolean =
        current == generation && (account == null || account.currentId() == owner)

    private fun acceptsAction(current: Int, owner: String?): Boolean =
        current == ownerGeneration && (account == null || account.currentId() == owner && observedOwner == owner)

    private fun observeShops(current: Int, owner: String?) = viewModelScope.launch {
        repository.observe().catch { error ->
            if (error is CancellationException) throw error
            emit(Result.failure(error))
        }.collect { result ->
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            if (accepts(current, owner)) updateState {
                copy(shops = result.getOrNull() ?: shops, isLoading = false, loadFailed = result.isFailure).withSettledInitialLoad()
            }
        }
    }

    private fun observeRoasters(source: FavoriteRoastersRepository, current: Int, owner: String?) = viewModelScope.launch {
        source.observe().catch { error ->
            if (error is CancellationException) throw error
            emit(Result.failure(error))
        }.collect { result ->
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            if (accepts(current, owner)) updateState {
                copy(roasters = result.getOrNull() ?: roasters, areRoastersLoading = false,
                    roastersLoadFailed = result.isFailure).withSettledInitialLoad()
            }
        }
    }

    private fun observeDrinks(source: FavoriteDrinksRepository, current: Int, owner: String?) = viewModelScope.launch {
        source.observe().catch { error ->
            if (error is CancellationException) throw error
            emit(Result.failure(error))
        }.collect { result ->
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            if (accepts(current, owner)) updateState {
                copy(drinks = result.getOrNull() ?: drinks, areDrinksLoading = false,
                    drinksLoadFailed = result.isFailure).withSettledInitialLoad()
            }
        }
    }

    private suspend fun removeDrink(id: String) {
        val source = drinkRepository ?: return
        if (!hasCurrentOwner() || id in currentState.removingDrinks || currentState.drinks.none { it.id == id && it.canRemove }) return
        val current = ownerGeneration
        val owner = observedOwner
        updateState { copy(removingDrinks = removingDrinks + id, actionFailed = false) }
        try {
            val result = source.remove(id)
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            if (acceptsAction(current, owner)) updateState { copy(actionFailed = result.isFailure) }
        } finally { if (acceptsAction(current, owner)) updateState { copy(removingDrinks = removingDrinks - id) } }
    }

    private suspend fun removeRoaster(id: String) {
        val source = roasterRepository ?: return
        if (!hasCurrentOwner() || id in currentState.removingRoasters || currentState.roasters.none { it.id == id }) return
        val current = ownerGeneration
        val owner = observedOwner
        updateState { copy(removingRoasters = removingRoasters + id, actionFailed = false) }
        try {
            val result = source.remove(id)
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            if (acceptsAction(current, owner)) updateState { copy(actionFailed = result.isFailure) }
        } finally {
            if (acceptsAction(current, owner)) updateState { copy(removingRoasters = removingRoasters - id) }
        }
    }

    private suspend fun remove(shopId: String) {
        if (!hasCurrentOwner() || shopId in currentState.removing || currentState.shops.none { it.id == shopId }) return
        val current = ownerGeneration
        val owner = observedOwner
        updateState { copy(removing = removing + shopId, actionFailed = false) }
        try {
            val result = repository.remove(shopId)
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            // Observation owns the list. Failed writes never optimistically erase cards.
            if (acceptsAction(current, owner)) updateState { copy(actionFailed = result.isFailure) }
        } finally {
            if (acceptsAction(current, owner)) updateState { copy(removing = removing - shopId) }
        }
    }
}
