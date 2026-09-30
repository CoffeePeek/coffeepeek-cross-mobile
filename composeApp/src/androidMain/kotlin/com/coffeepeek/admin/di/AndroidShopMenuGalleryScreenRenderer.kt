package com.coffeepeek.admin.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.ui.screen.shop.ShopMenuGalleryScreenRenderer
import com.coffeepeek.feature.shop.api.ShopMenuGalleryEntry

internal class AndroidShopMenuGalleryScreenRenderer(
    private val entry: ShopMenuGalleryEntry,
) : ShopMenuGalleryScreenRenderer {
    @Composable
    override fun Content(shopId: String) {
        var viewer by remember(shopId) { mutableStateOf<Pair<List<String>, Int>?>(null) }
        entry.Content(shopId, onBack = Navigator::popBack,
            onOpenPhoto = { urls, index -> viewer = urls to index })
        viewer?.let { (urls, index) ->
            FullScreenImageDialog(urls, index, onDismiss = { viewer = null })
        }
    }
}
