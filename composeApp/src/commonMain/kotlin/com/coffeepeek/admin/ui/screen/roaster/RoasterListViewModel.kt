package com.coffeepeek.admin.ui.screen.roaster

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal data class RoasterListItem(val catalog: CatalogItem, val details: RoasterDetails? = null) {
    val routeId: String? get() = catalog.address?.slug?.takeIf(String::isNotBlank)
}

internal data class RoasterListUiState(
    val items: List<RoasterListItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val savingFavoriteIds: Set<String> = emptySet(),
    val actionMessage: String? = null,
) {
    val visibleItems: List<RoasterListItem>
        get() = items.filter { it.catalog.name.contains(query.trim(), ignoreCase = true) }
}

internal class RoasterListViewModel(
    private val shops: ShopRepository,
    private val roasters: RoasterRepository,
    private val favorites: RoasterFavorites,
    private val sessions: SessionRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(RoasterListUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        favorites.observeFavorites()
            .onEach { favorites -> _state.update { it.copy(favoriteIds = favorites.map { it.roasterFavoriteId }.toSet()) } }
            .catch { _state.update { it.copy(actionMessage = "Не удалось загрузить избранное") } }
            .launchIn(workScope)
        refresh()
    }

    fun clearActionMessage() { _state.update { it.copy(actionMessage = null) } }

    fun toggleFavorite(item: RoasterListItem) {
        val id = item.catalog.roasterFavoriteId
        val current = _state.value
        if (id in current.savingFavoriteIds) return
        _state.update { it.copy(savingFavoriteIds = it.savingFavoriteIds + id) }
        workScope.launch {
            try {
                if (!sessions.isLoggedIn()) {
                    Navigator.navigate(Navigator.Screen.Auth)
                    return@launch
                }
                val snapshot = item.catalog.copy(
                    photoUrl = item.catalog.photoUrl ?: item.details?.photos?.firstOrNull()?.fullUrl,
                )
                favorites.setFavorite(snapshot, id !in current.favoriteIds)
                    .onFailure { _state.update { it.copy(actionMessage = "Не удалось изменить избранное") } }
            } finally {
                _state.update { it.copy(savingFavoriteIds = it.savingFavoriteIds - id) }
            }
        }
    }

    fun onQueryChange(query: String) { _state.update { it.copy(query = query) } }

    fun refresh() {
        loadJob?.cancel()
        loadJob = workScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = shops.getCatalogs()
            currentCoroutineContext().ensureActive()
            result.onSuccess { catalogs ->
                _state.update { it.copy(items = catalogs.roasters.map(::RoasterListItem)) }
                // ponytail: catalog + one detail request per roaster; use a summary endpoint when the catalog grows.
                val requests = Semaphore(4)
                catalogs.roasters.map { catalog ->
                    async {
                        requests.withPermit {
                            val routeId = catalog.address?.slug?.takeIf(String::isNotBlank) ?: return@withPermit
                            val details = roasters.getRoaster(routeId).getOrNull()
                            currentCoroutineContext().ensureActive()
                            _state.update { state ->
                                state.copy(
                                    items = state.items.map { if (it.catalog.id == catalog.id) it.copy(details = details) else it },
                                    error = if (details == null) "Некоторые сведения об обжарщиках не удалось загрузить" else state.error,
                                )
                            }
                        }
                    }
                }.awaitAll()
                _state.update { it.copy(isLoading = false) }
            }.onFailure {
                _state.update { it.copy(isLoading = false, error = "Не удалось загрузить обжарщиков") }
            }
        }
    }
}
