package com.coffeepeek.admin.ui.screen.favorites

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val shops: List<CoffeeShopDetails> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val roasters: List<CatalogItem> = emptyList(),
    val areRoastersLoading: Boolean = true,
    val savingRoasterIds: Set<String> = emptySet(),
    val actionMessage: String? = null,
)

class FavoritesViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val roasterFavorites: RoasterFavorites,
) : BaseViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state = _state.asStateFlow()

    init {
        roasterFavorites.observeFavorites()
            .onEach { roasters -> _state.update { it.copy(roasters = roasters, areRoastersLoading = false) } }
            .catch { _state.update { it.copy(areRoastersLoading = false, actionMessage = "Не удалось загрузить обжарщиков") } }
            .launchIn(workScope)
        load()
        FavoriteSync.changes
            .onEach { load(force = true) }
            .launchIn(workScope)
    }

    fun clearActionMessage() { _state.update { it.copy(actionMessage = null) } }

    fun removeRoaster(roaster: CatalogItem) {
        val id = roaster.roasterFavoriteId
        if (id in _state.value.savingRoasterIds) return
        _state.update { it.copy(savingRoasterIds = it.savingRoasterIds + id) }
        workScope.launch {
            try {
                roasterFavorites.setFavorite(roaster, false)
                    .onFailure { _state.update { it.copy(actionMessage = "Не удалось убрать обжарщика из избранного") } }
            } finally {
                _state.update { it.copy(savingRoasterIds = it.savingRoasterIds - id) }
            }
        }
    }

    fun load(force: Boolean = false) {
        if (!force && (_state.value.isLoading || _state.value.shops.isNotEmpty())) return
        workScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            favoriteRepository.getFavorites()
                .onSuccess { shops -> _state.update { it.copy(shops = shops, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    /** Tapping the heart on a favorites card unfavorites it and drops it from the list. */
    fun removeFavorite(shop: CoffeeShop) {
        workScope.launch {
            _state.update { it.copy(shops = it.shops.filterNot { d -> d.shop.id == shop.id }) }
            favoriteRepository.removeFavorite(shop.id)
                .onSuccess { FavoriteSync.notifyChanged(shop.id, false) }
                .onFailure { load(force = true) }
        }
    }
}
