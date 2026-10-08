package com.coffeepeek.admin.ui.screen.feed

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.settings.CityPreference
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.favorites.withFavoriteMembership
import com.coffeepeek.admin.utils.FavoriteSync
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.City
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.ShopFilters
import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.favorites.domain.usecase.ObserveFavoriteIdsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val PAGE_SIZE = 20

data class FeedFiltersUi(
    val cityId: String? = null,
    val coffeeFocus: String? = null,
    val openOnly: Boolean = false,
    val newOnly: Boolean = false,
    val visitedOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val nearbyOnly: Boolean = false,
    val priceRange: Int? = null,
    val minRating: Double? = null,
    val roasterIds: Set<String> = emptySet(),
    val beanIds: Set<String> = emptySet(),
    val equipmentIds: Set<String> = emptySet(),
    val brewMethodIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (coffeeFocus != null) count++
            if (openOnly) count++
            if (newOnly) count++
            if (visitedOnly) count++
            if (favoritesOnly) count++
            if (nearbyOnly) count++
            if (priceRange != null) count++
            if (minRating != null) count++
            count += roasterIds.size + beanIds.size + equipmentIds.size +
                brewMethodIds.size + tagIds.size
            return count
        }

    fun clearSelections(): FeedFiltersUi = FeedFiltersUi(cityId = cityId)
}

data class FeedUiState(
    val shops: List<CoffeeShop> = emptyList(),
    val favoriteUpdates: Set<String> = emptySet(),
    // The first shops request starts after catalogs/city resolution. Keep the
    // screen in a loading state during that preparation so the empty state
    // cannot flash before the initial request is dispatched.
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val isSearchActive: Boolean = false,
    val filters: FeedFiltersUi = FeedFiltersUi(),
    val cities: List<City> = emptyList(),
    val beans: List<CatalogItem> = emptyList(),
    val equipment: List<CatalogItem> = emptyList(),
    val roasters: List<CatalogItem> = emptyList(),
    val brewMethods: List<CatalogItem> = emptyList(),
    val shopTags: List<CatalogItem> = emptyList(),
    val showFilters: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasMore: Boolean = false,
) {
    val showDiscovery: Boolean
        get() = !isSearchActive && query.isBlank() && activeFilterCount == 0

    val activeFilterCount: Int
        get() = filters.activeFilterCount

    val visibleShops: List<CoffeeShop>
        get() = shops.filter { shop ->
            (!filters.openOnly || shop.isOpen) &&
                (!filters.newOnly || shop.isNew) &&
                (!filters.visitedOnly || shop.isVisited) &&
                (!filters.favoritesOnly || shop.isFavorite)
        }
}

@OptIn(FlowPreview::class)
class FeedViewModel(
    private val shopRepository: ShopRepository,
    private val favoriteRepository: FavoriteRepository,
    private val cityPreference: CityPreference,
    private val sessionRepository: SessionRepository,
    private val observeFavoriteIds: ObserveFavoriteIdsUseCase? = null,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private val favoriteIds = MutableStateFlow<Set<String>?>(null)
    private val favoriteMutationGuard = Mutex()
    private var shopsLoadJob: Job? = null
    private var isCityReady = false

    init {
        loadCatalogs()
        cityPreference.selectedCityId
            .onEach { cityId ->
                if (!isCityReady || cityId == null || cityId == _uiState.value.filters.cityId) {
                    return@onEach
                }
                _uiState.update { it.copy(filters = it.filters.copy(cityId = cityId)) }
                loadShops(reset = true)
            }
            .launchIn(workScope)
        queryFlow
            .debounce(400)
            .distinctUntilChanged()
            .drop(1)
            .onEach { query ->
                loadShops(reset = true)
            }
            .launchIn(workScope)

        if (observeFavoriteIds == null) {
            FavoriteSync.changes
                .onEach { change ->
                    _uiState.update { state ->
                        state.copy(shops = state.shops.map { shop ->
                            if (shop.id == change.shopId) shop.copy(isFavorite = change.isFavorite) else shop
                        })
                    }
                }
                .launchIn(workScope)
        } else {
            observeFavoriteIds()
                .onEach { result ->
                    result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                    val ids = result.getOrNull() ?: return@onEach
                    favoriteIds.value = ids
                    _uiState.update { state ->
                        state.copy(shops = state.shops.map { it.withFavoriteMembership(ids) })
                    }
                }
                .launchIn(workScope)
        }
    }

    private fun loadCatalogs() {
        if (_uiState.value.cities.isNotEmpty()) return
        workScope.launch {
            shopRepository.getCatalogs()
                .onSuccess { catalogs ->
                    val cityId = cityPreference.resolve(catalogs.cities)
                    _uiState.update {
                        it.copy(
                            filters = it.filters.copy(cityId = cityId),
                            cities = catalogs.cities,
                            beans = catalogs.beans,
                            equipment = catalogs.equipment,
                            roasters = catalogs.roasters,
                            brewMethods = catalogs.brewMethods,
                            shopTags = catalogs.shopTags,
                        )
                    }
                    isCityReady = true
                    loadShops(reset = true)
                }
                .onFailure {
                    isCityReady = true
                    loadShops(reset = true)
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun activateSearch() {
        _uiState.update { it.copy(isSearchActive = true) }
    }

    fun cancelSearch() {
        if (_uiState.value.activeFilterCount > 0) clearFilters() else onQueryChange("")
        _uiState.update { it.copy(isSearchActive = false) }
    }

    fun toggleFilters() {
        val willShow = !_uiState.value.showFilters
        _uiState.update { it.copy(showFilters = willShow) }
        if (willShow && _uiState.value.cities.isEmpty() && _uiState.value.beans.isEmpty()) {
            loadCatalogs()
        }
    }

    fun closeFilters() {
        _uiState.update { it.copy(showFilters = false) }
    }

    fun applyFilters(filters: FeedFiltersUi) {
        _uiState.update { it.copy(filters = filters) }
        loadShops(reset = true)
    }

    fun setCoffeeFocus(coffeeFocus: String?) {
        _uiState.update { it.copy(filters = it.filters.copy(coffeeFocus = coffeeFocus)) }
        loadShops(reset = true)
    }

    fun toggleOpenOnly() {
        _uiState.update { it.copy(filters = it.filters.copy(openOnly = !it.filters.openOnly)) }
    }

    fun toggleNewOnly() {
        _uiState.update { it.copy(filters = it.filters.copy(newOnly = !it.filters.newOnly)) }
    }

    fun toggleVisitedOnly() {
        workScope.launch {
            if (!sessionRepository.isLoggedIn()) {
                Navigator.navigate(Navigator.Screen.Auth)
                return@launch
            }
            _uiState.update { it.copy(filters = it.filters.copy(visitedOnly = !it.filters.visitedOnly)) }
        }
    }

    fun toggleFavoritesOnly() {
        workScope.launch {
            if (!sessionRepository.isLoggedIn()) {
                Navigator.navigate(Navigator.Screen.Auth)
                return@launch
            }
            _uiState.update { it.copy(filters = it.filters.copy(favoritesOnly = !it.filters.favoritesOnly)) }
        }
    }

    fun toggleNearbyOnly() {
        _uiState.update { it.copy(filters = it.filters.copy(nearbyOnly = !it.filters.nearbyOnly)) }
    }

    fun setPriceRange(priceRange: Int?) {
        _uiState.update { it.copy(filters = it.filters.copy(priceRange = priceRange)) }
        loadShops(reset = true)
    }

    fun setMinRating(rating: Double?) {
        _uiState.update { it.copy(filters = it.filters.copy(minRating = rating)) }
        loadShops(reset = true)
    }

    fun toggleFilterCatalog(
        type: String,
        id: String,
    ) {
        _uiState.update { state ->
            val filters = when (type) {
                "roaster" -> state.filters.copy(
                    roasterIds = state.filters.roasterIds.toggle(id),
                )
                "bean" -> state.filters.copy(
                    beanIds = state.filters.beanIds.toggle(id),
                )
                "equipment" -> state.filters.copy(
                    equipmentIds = state.filters.equipmentIds.toggle(id),
                )
                "brew" -> state.filters.copy(
                    brewMethodIds = state.filters.brewMethodIds.toggle(id),
                )
                "tag" -> state.filters.copy(
                    tagIds = state.filters.tagIds.toggle(id),
                )
                else -> state.filters
            }
            state.copy(filters = filters)
        }
        loadShops(reset = true)
    }

    fun clearFilters() {
        queryFlow.value = ""
        _uiState.update {
            it.copy(query = "", filters = it.filters.clearSelections())
        }
        loadShops(reset = true)
    }

    fun loadMore() {
        loadShops(reset = false)
    }

    fun refresh() {
        if (_uiState.value.isRefreshing || _uiState.value.isLoading) return
        loadShops(reset = true)
    }

    fun toggleFavorite(shop: CoffeeShop) {
        workScope.launch {
            if (!sessionRepository.isLoggedIn()) {
                Navigator.navigate(Navigator.Screen.Auth)
                return@launch
            }
            val started = favoriteMutationGuard.withLock {
                if (shop.id in _uiState.value.favoriteUpdates) {
                    false
                } else {
                    _uiState.update { it.copy(favoriteUpdates = it.favoriteUpdates + shop.id) }
                    true
                }
            }
            if (!started) return@launch

            val previousFavorite = _uiState.value.shops
                .firstOrNull { it.id == shop.id }?.isFavorite ?: shop.isFavorite
            val nextFavorite = !previousFavorite
            _uiState.update { state ->
                state.copy(
                    shops = state.shops.map { item ->
                        if (item.id == shop.id) item.copy(isFavorite = nextFavorite) else item
                    },
                )
            }

            try {
                val result = if (nextFavorite) {
                    favoriteRepository.addFavorite(shop, shop.address)
                } else {
                    favoriteRepository.removeFavorite(shop.id)
                }
                result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                if (result.isSuccess) {
                    FavoriteSync.notifyChanged(shop.id, nextFavorite)
                } else {
                    restoreFavorite(shop.id, previousFavorite, nextFavorite)
                }
            } catch (cancellation: CancellationException) {
                restoreFavorite(shop.id, previousFavorite, nextFavorite)
                throw cancellation
            } catch (error: Exception) {
                restoreFavorite(shop.id, previousFavorite, nextFavorite)
                throw error
            } finally {
                favoriteMutationGuard.withLock {
                    _uiState.update { it.copy(favoriteUpdates = it.favoriteUpdates - shop.id) }
                }
            }
        }
    }

    private fun restoreFavorite(shopId: String, previousFavorite: Boolean, requestedFavorite: Boolean) {
        val observedFavorite = favoriteIds.value?.contains(shopId)
        _uiState.update { state ->
            state.copy(
                shops = state.shops.map { item ->
                    if (item.id == shopId) {
                        val latestFavorite = observedFavorite ?: item.isFavorite
                        val restoredFavorite = if (latestFavorite == requestedFavorite) {
                            previousFavorite
                        } else {
                            latestFavorite
                        }
                        item.copy(isFavorite = restoredFavorite)
                    } else item
                },
            )
        }
    }

    private fun loadShops(reset: Boolean) {
        val snapshot = _uiState.value
        if (!reset) {
            if (snapshot.isLoadingMore || !snapshot.hasMore || snapshot.isLoading || snapshot.isRefreshing) {
                return
            }
        }

        shopsLoadJob?.cancel()
        shopsLoadJob = workScope.launch {
            val page = if (reset) 1 else _uiState.value.currentPage + 1

            _uiState.update { state ->
                when {
                    reset && state.shops.isEmpty() ->
                        state.copy(isLoading = true, isRefreshing = false, isLoadingMore = false, error = null)
                    reset ->
                        state.copy(isRefreshing = true, isLoading = false, isLoadingMore = false, error = null)
                    else ->
                        state.copy(isLoadingMore = true, error = null)
                }
            }

            val current = _uiState.value
            val filters = current.filters
            shopRepository.searchShops(
                ShopFilters(
                    query = current.query.trim().takeIf { it.isNotBlank() },
                    cityId = filters.cityId,
                    coffeeFocus = filters.coffeeFocus,
                    roasterIds = filters.roasterIds.toList(),
                    beanIds = filters.beanIds.toList(),
                    equipmentIds = filters.equipmentIds.toList(),
                    brewMethodIds = filters.brewMethodIds.toList(),
                    tagIds = filters.tagIds.toList(),
                    priceRange = filters.priceRange,
                    minRating = filters.minRating,
                    page = page,
                    pageSize = PAGE_SIZE,
                ),
            ).onSuccess { result ->
                if (!isActive) return@onSuccess
                _uiState.update { state ->
                    state.copy(
                        shops = (if (reset) result.items else state.shops + result.items).let { shops ->
                            favoriteIds.value?.let { ids -> shops.map { it.withFavoriteMembership(ids) } } ?: shops
                        },
                        currentPage = result.currentPage,
                        totalPages = result.totalPages,
                        hasMore = result.currentPage < result.totalPages,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = null,
                    )
                }
            }.onFailure { e ->
                if (!isActive) return@onFailure
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = e.message,
                    )
                }
            }
        }
    }

    private fun Set<String>.toggle(id: String): Set<String> =
        if (contains(id)) this - id else this + id
}
