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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoasterDetailUiState(
    val isLoading: Boolean = true,
    val details: RoasterDetails? = null,
    val error: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val isFavoriteLoading: Boolean = false,
    val actionMessage: String? = null,
) {
    val isFavorite: Boolean get() = details?.let { (it.publicAddress?.slug ?: it.id) in favoriteIds } == true
}

class RoasterDetailViewModel(
    private val roasterId: String,
    private val repository: RoasterRepository,
    private val shopRepository: ShopRepository,
    private val favorites: RoasterFavorites,
    private val sessions: SessionRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(RoasterDetailUiState())
    val state = _state.asStateFlow()

    init {
        favorites.observeFavorites()
            .onEach { favorites -> _state.update { it.copy(favoriteIds = favorites.map { it.roasterFavoriteId }.toSet()) } }
            .catch { _state.update { it.copy(actionMessage = "Не удалось загрузить избранное") } }
            .launchIn(workScope)
        load()
    }

    fun clearActionMessage() { _state.update { it.copy(actionMessage = null) } }

    fun toggleFavorite() {
        val current = _state.value
        val details = current.details ?: return
        if (current.isFavoriteLoading) return
        _state.update { it.copy(isFavoriteLoading = true) }
        workScope.launch {
            try {
                if (!sessions.isLoggedIn()) {
                    Navigator.navigate(Navigator.Screen.Auth)
                    return@launch
                }
                val roaster = CatalogItem(
                    id = details.id,
                    name = details.name,
                    photoUrl = details.photos.firstOrNull()?.fullUrl,
                    address = details.publicAddress,
                )
                favorites.setFavorite(roaster, !current.isFavorite)
                    .onFailure { _state.update { it.copy(actionMessage = "Не удалось изменить избранное") } }
            } finally {
                _state.update { it.copy(isFavoriteLoading = false) }
            }
        }
    }

    fun load() {
        workScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.getRoaster(roasterId)
                .onSuccess { details ->
                    _state.update { it.copy(isLoading = false, details = details) }
                    val enrichedDetails = enrichShopPhotos(details)
                    _state.update { state ->
                        val currentDetails = state.details
                        if (currentDetails?.id == enrichedDetails.id) {
                            state.copy(details = currentDetails.copy(shops = enrichedDetails.shops))
                        } else {
                            state
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Не удалось загрузить обжарщика",
                        )
                    }
                }
        }
    }

    private suspend fun enrichShopPhotos(details: RoasterDetails): RoasterDetails = coroutineScope {
        val shops = details.shops.map { shop ->
            async {
                if (!shop.photoUrl.isNullOrBlank()) return@async shop
                val shopDetails = shopRepository.getShopDetails(shop.id).getOrNull()
                val photoUrl = shopDetails
                    ?.photos
                    ?.firstOrNull(String::isNotBlank)
                    ?: shopDetails?.shop?.photoUrl?.takeIf(String::isNotBlank)
                shop.copy(photoUrl = photoUrl)
            }
        }.awaitAll()
        details.copy(shops = shops)
    }
}
