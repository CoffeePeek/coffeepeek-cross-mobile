package com.coffeepeek.admin.ui.screen.roaster

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.domain.model.RoasterDetails
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.ShopRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoasterDetailUiState(
    val isLoading: Boolean = true,
    val details: RoasterDetails? = null,
    val error: String? = null,
)

class RoasterDetailViewModel(
    private val roasterId: String,
    private val repository: RoasterRepository,
    private val shopRepository: ShopRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(RoasterDetailUiState())
    val state = _state.asStateFlow()

    init { load() }

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
