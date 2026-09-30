package com.coffeepeek.feature.shop.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.shop.api.ShopMenuGalleryEntry
import com.coffeepeek.feature.shop.domain.repository.ShopMenuGalleryRepository
import com.coffeepeek.feature.shop.impl.ui.ShopMenuGalleryViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.ShopMenuGalleryScreen

fun createShopMenuGalleryEntry(repository: ShopMenuGalleryRepository): ShopMenuGalleryEntry =
    DefaultShopMenuGalleryEntry(repository)

private class DefaultShopMenuGalleryEntry(
    private val repository: ShopMenuGalleryRepository,
) : ShopMenuGalleryEntry {
    @Composable
    override fun Content(
        shopId: String,
        onBack: () -> Unit,
        onOpenPhoto: (List<String>, Int) -> Unit,
    ) {
        val viewModel = viewModel { ShopMenuGalleryViewModel(shopId, repository) }
        ShopMenuGalleryScreen(viewModel, onBack, onOpenPhoto)
    }
}
