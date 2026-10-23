package com.coffeepeek.admin.ui.screen.roaster

import com.coffeepeek.admin.analytics.*

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.admin.feature.favorites.api.roasterFavoriteId
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.model.RoasterSummary
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

internal data class RoasterListItem(
    val catalog: CatalogItem,
    val details: RoasterDetails? = null,
) {
    constructor(summary: RoasterSummary) : this(catalog = summary.toCatalogItem())

    val routeId: String? get() = catalog.address?.slug?.takeIf(String::isNotBlank)
}

internal data class RoasterListUiState(
    val items: List<RoasterListItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val savingFavoriteIds: Set<String> = emptySet(),
    val actionMessage: String? = null,
    val selectedTagIds: Set<String> = emptySet(),
) {
    val availableTags get() = items.flatMap { it.catalog.tags }
        .distinctBy { it.slug }.sortedBy { it.sortOrder }

    fun visibleItems(
        query: String,
        selectedRoasterIds: Set<String> = emptySet(),
        favoritesOnly: Boolean = false,
    ): List<RoasterListItem> = items.filter { item ->
        item.catalog.name.contains(query.trim(), ignoreCase = true) &&
            (selectedRoasterIds.isEmpty() || item.catalog.id in selectedRoasterIds || item.routeId in selectedRoasterIds) &&
            (!favoritesOnly || item.catalog.roasterFavoriteId in favoriteIds) &&
            selectedTagIds.all { tag -> item.catalog.tags.any { it.slug == tag } }
    }
}

internal class RoasterListViewModel(
    private val roasters: RoasterRepository,
    private val favorites: RoasterFavorites,
    private val sessions: SessionRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(RoasterListUiState())
    val state = _state.asStateFlow()
    private val searchAnalytics = SearchTracker(Catalog.Roasters)
    private var loadJob: Job? = null
    private val detailLocksMutex = Mutex()
    private val detailLocks = mutableMapOf<String, Mutex>()
    private val detailRequests = Semaphore(4)
    fun resultsShown(query: String, selected: Set<String>, favoritesOnly: Boolean) {
        val current = state.value
        if (current.isLoading) return
        val keys = buildList { if (selected.isNotEmpty()) add("roasters"); if (current.selectedTagIds.isNotEmpty()) add("tags"); if (favoritesOnly) add("favoritesOnly") }
        val search = searchAnalytics.begin(listOf(query.trim().lowercase(), selected, favoritesOnly, current.selectedTagIds), query, keys)
        if (current.error != null) search?.failed(SafeAnalyticsError("unknown"))
        else search?.results(current.visibleItems(query, selected, favoritesOnly).size)
    }
    fun openedRoaster(index: Int) = searchAnalytics.open(Entity.Roaster, index + 1)


    init {
        favorites.observeErrors()
            .onEach { error -> if (error != null) _state.update { it.copy(actionMessage = error) } }
            .launchIn(workScope)
        favorites.observeFavorites()
            .onEach { favorites -> _state.update { it.copy(favoriteIds = favorites.map { it.roasterFavoriteId }.toSet()) } }
            .catch { _state.update { it.copy(actionMessage = "Не удалось загрузить избранное") } }
            .launchIn(workScope)
        load(explicit = false)
    }

    fun clearActionMessage() { _state.update { it.copy(actionMessage = null) } }

    fun toggleTag(slug: String) {
        _state.update { state ->
            state.copy(selectedTagIds = if (slug in state.selectedTagIds) state.selectedTagIds - slug else state.selectedTagIds + slug)
        }
    }

    fun clearTags() { _state.update { it.copy(selectedTagIds = emptySet()) } }

    suspend fun loadDetails(slug: String?) {
        if (slug.isNullOrBlank()) return
        val lock = detailLocksMutex.withLock { detailLocks.getOrPut(slug) { Mutex() } }
        lock.withLock {
            if (_state.value.items.none { it.routeId == slug && it.details == null }) return
            val details = detailRequests.withPermit { roasters.getRoaster(slug).getOrNull() }
            currentCoroutineContext().ensureActive()
            if (details != null) {
                _state.update { state ->
                    state.copy(items = state.items.map { item ->
                        if (item.routeId == slug) item.copy(details = details) else item
                    })
                }
            }
        }
    }

    fun toggleFavorite(item: RoasterListItem) {
        val id = item.catalog.roasterFavoriteId
        val desired = id !in _state.value.favoriteIds
        com.coffeepeek.admin.feature.favorites.api.FavoriteInteractions.change(workScope, id, true, desired,
            action = { favorites.setFavorite(item.catalog.copy(photoUrl = item.catalog.photoUrl ?: item.details?.photos?.firstOrNull()?.fullUrl), desired) },
            completed = { result -> result.onFailure { _state.update { it.copy(actionMessage = "Не удалось изменить избранное") } } },
        )
    }

    fun refresh() = load(explicit = true)

    private fun load(explicit: Boolean) {
        loadJob?.cancel()
        loadJob = workScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = if (explicit) roasters.refreshRoasters() else roasters.getRoasters()
            currentCoroutineContext().ensureActive()
            result.onSuccess { catalog ->
                _state.update { it.copy(items = catalog.map(::RoasterListItem), isLoading = false) }
            }.onFailure {
                _state.update { it.copy(isLoading = false, error = "Не удалось загрузить обжарщиков") }
            }
        }
    }
}
