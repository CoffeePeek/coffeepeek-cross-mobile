package com.coffeepeek.feature.shop.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.shop.domain.repository.ShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.ui.ShopMenuGalleryViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun createIosShopMenuGallerySession(shopId: String, repository: ShopMenuGalleryRepository): IosShopMenuGallerySession =
    IosShopMenuGallerySession(ShopMenuGalleryViewModel(shopId, repository))

data class ShopMenuGallerySnapshot(
    val shopTitle: String,
    val previewUrls: List<String>,
    val isLoading: Boolean,
    val hasError: Boolean,
)

/** One instance belongs to one presented gallery. Closing releases the ViewModel and its work. */
class IosShopMenuGallerySession internal constructor(private val viewModel: ShopMenuGalleryViewModel) {
    private val store = ViewModelStore().apply { put("shop-menu-gallery", viewModel) }

    fun currentState(): ShopMenuGallerySnapshot = viewModel.state.value.toSnapshot()

    fun observeState(onState: (ShopMenuGallerySnapshot) -> Unit): Job = viewModel.viewModelScope.launch {
        viewModel.state.collect { onState(it.toSnapshot()) }
    }

    fun observeEvents(onBack: () -> Unit, onPhoto: (List<String>, Int) -> Unit): Job = viewModel.viewModelScope.launch {
        viewModel.events.collect { event ->
            when (event) {
                ShopMenuGalleryEvent.Back -> onBack()
                is ShopMenuGalleryEvent.OpenPhoto -> onPhoto(event.imageUrls, event.initialIndex)
            }
        }
    }

    fun retry() = viewModel.onAction(ShopMenuGalleryAction.Load)

    fun openPhoto(index: Int) = viewModel.onAction(ShopMenuGalleryAction.OpenPhoto(index))

    fun back() = viewModel.onAction(ShopMenuGalleryAction.Back)

    fun close() = store.clear()
}

private fun ShopMenuGalleryState.toSnapshot() =
    ShopMenuGallerySnapshot(shopTitle, photos.map { it.previewUrl }, isLoading, hasError)
