package com.coffeepeek.admin.ui.screen.roaster

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
    private var loadJob: Job? = null

    init {
        favorites.observeFavorites()
            .onEach { favorites -> _state.update { it.copy(favoriteIds = favorites.map { it.roasterFavoriteId }.toSet()) } }
            .catch { _state.update { it.copy(actionMessage = "Не удалось загрузить избранное") } }
            .launchIn(workScope)
        refresh()
    }

    fun clearActionMessage() { _state.update { it.copy(actionMessage = null) } }

    fun toggleTag(slug: String) {
        _state.update { state ->
            state.copy(selectedTagIds = if (slug in state.selectedTagIds) state.selectedTagIds - slug else state.selectedTagIds + slug)
        }
    }

    fun clearTags() { _state.update { it.copy(selectedTagIds = emptySet()) } }

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

    fun refresh() {
        loadJob?.cancel()
        loadJob = workScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = roasters.getRoasters()
            currentCoroutineContext().ensureActive()
            result.onSuccess { catalog ->
                _state.update { it.copy(items = catalog.map(::RoasterListItem), isLoading = false) }
            }.onFailure {
                _state.update { it.copy(isLoading = false, error = "Не удалось загрузить обжарщиков") }
            }
        }
    }
}
