package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.shop.domain.repository.ShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryState
import kotlinx.coroutines.CancellationException

internal class ShopMenuGalleryViewModel(
    private val shopId: String,
    private val repository: ShopMenuGalleryRepository,
) : MviViewModel<ShopMenuGalleryState, ShopMenuGalleryAction, ShopMenuGalleryEvent>(ShopMenuGalleryState()) {
    private var loadInProgress = false
    init { onAction(ShopMenuGalleryAction.Load) }

    override suspend fun handleActionInternal(action: ShopMenuGalleryAction) {
        when (action) {
            ShopMenuGalleryAction.Load -> load()
            ShopMenuGalleryAction.Back -> sendEvent(ShopMenuGalleryEvent.Back)
            is ShopMenuGalleryAction.OpenPhoto -> {
                val photos = currentState.photos
                if (action.index in photos.indices) {
                    sendEvent(ShopMenuGalleryEvent.OpenPhoto(photos.map { it.fullUrl }, action.index))
                }
            }
        }
    }

    private suspend fun load() {
        if (loadInProgress) return
        loadInProgress = true
        updateState { copy(isLoading = true, hasError = false) }
        try {
            val result = repository.getMenuGallery(shopId)
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            result.fold(
                onSuccess = { gallery -> updateState {
                    copy(shopTitle = gallery.shopTitle, photos = gallery.photos, hasError = false)
                } },
                onFailure = { updateState { copy(hasError = true) } },
            )
        } finally {
            loadInProgress = false
            updateState { copy(isLoading = false) }
        }
    }
}
