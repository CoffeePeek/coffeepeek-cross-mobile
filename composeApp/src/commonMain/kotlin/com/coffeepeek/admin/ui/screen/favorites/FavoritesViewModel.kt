package com.coffeepeek.admin.ui.screen.favorites

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.RoasterRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class FavoritesUiState(
    val shops: List<CoffeeShopDetails> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val roasters: List<CatalogItem> = emptyList(),
    val roasterDetails: Map<String, RoasterDetails> = emptyMap(),
    val areRoastersLoading: Boolean = true,
    val savingRoasterIds: Set<String> = emptySet(),
    val actionMessage: String? = null,
    val roasterError: String? = null,
    val drinks: List<FavoriteEntry> = emptyList(),
    val removingDrinkIds: Set<String> = emptySet(),
)

class FavoritesViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val roasterFavorites: RoasterFavorites,
    private val roasterRepository: RoasterRepository,
    private val serverFavorites: FavoritesRepository? = null,
) : BaseViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state = _state.asStateFlow()

    init {
        serverFavorites?.observeFavorites()
            ?.onEach { result -> result.onSuccess { entries ->
                _state.update { it.copy(drinks = entries.filter { entry -> entry.kind == FavoriteKind.Drink }) }
            } }
            ?.catch { _state.update { it.copy(error = "Не удалось загрузить избранное") } }
            ?.launchIn(workScope)
        roasterFavorites.observeErrors()
            .onEach { error -> _state.update { it.copy(roasterError = error) } }
            .launchIn(workScope)
        workScope.launch {
            roasterFavorites.observeFavorites()
                .catch { _state.update { it.copy(areRoastersLoading = false, actionMessage = "Не удалось загрузить обжарщиков") } }
                .collectLatest { roasters ->
                    val ids = roasters.map { it.roasterFavoriteId }.toSet()
                    _state.update { it.copy(
                        roasters = roasters,
                        areRoastersLoading = false,
                        roasterDetails = it.roasterDetails.filterKeys { id -> id in ids },
                    ) }
                    val requests = Semaphore(4)
                    coroutineScope {
                        roasters.filter { it.roasterFavoriteId !in _state.value.roasterDetails }.forEach { roaster ->
                            val slug = roaster.address?.slug ?: return@forEach
                            launch {
                                val details = requests.withPermit { roasterRepository.getRoaster(slug).getOrNull() }
                                currentCoroutineContext().ensureActive()
                                if (details != null) {
                                    _state.update { it.copy(roasterDetails = it.roasterDetails + (roaster.roasterFavoriteId to details)) }
                                }
                            }
                        }
                    }
                }
        }
        load()
        FavoriteSync.changes
            .onEach { load(force = true) }
            .launchIn(workScope)
    }

    fun clearActionMessage() { _state.update { it.copy(actionMessage = null) } }

    fun removeDrink(drink: FavoriteEntry) {
        val repository = serverFavorites ?: return
        if (drink.kind != FavoriteKind.Drink || drink.id in _state.value.removingDrinkIds) return
        _state.update { it.copy(removingDrinkIds = it.removingDrinkIds + drink.id) }
        workScope.launch {
            try {
                repository.setFavorite(drink, false).onSuccess {
                    _state.update { state -> state.copy(drinks = state.drinks.filterNot { it.id == drink.id }) }
                }.onFailure {
                    _state.update { it.copy(actionMessage = "Не удалось изменить избранное") }
                }
            } finally {
                _state.update { it.copy(removingDrinkIds = it.removingDrinkIds - drink.id) }
            }
        }
    }

    fun removeRoaster(roaster: CatalogItem) {
        com.coffeepeek.admin.feature.favorites.api.FavoriteInteractions.change(workScope, roaster.roasterFavoriteId, true, false,
            action = { roasterFavorites.setFavorite(roaster, false) },
            completed = { result -> result.onFailure { _state.update { it.copy(actionMessage = "Не удалось изменить избранное") } } },
        )
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
        com.coffeepeek.admin.feature.favorites.api.FavoriteInteractions.change(workScope, shop.id, false, false,
            action = { favoriteRepository.removeFavorite(shop.id) },
            completed = { result -> result
                .onSuccess { FavoriteSync.notifyChanged(shop.id, false); load(force = true) }
                .onFailure { _state.update { it.copy(actionMessage = "Не удалось изменить избранное") } }
            },
        )
    }
}
