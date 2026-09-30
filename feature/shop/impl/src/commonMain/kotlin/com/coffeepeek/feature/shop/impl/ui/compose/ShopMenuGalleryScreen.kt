package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CpTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.MenuPhoto
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_menu_gallery_back
import com.coffeepeek.feature.shop.impl.resources.shop_menu_gallery_title
import com.coffeepeek.feature.shop.impl.ui.ShopMenuGalleryViewModel
import com.coffeepeek.feature.shop.impl.ui.compose.component.GalleryMessage
import com.coffeepeek.feature.shop.impl.ui.compose.component.MenuPhotoCard
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryEvent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopMenuGalleryState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopMenuGalleryScreen(
    viewModel: ShopMenuGalleryViewModel,
    onBack: () -> Unit,
    onOpenPhoto: (List<String>, Int) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentBack by rememberUpdatedState(onBack)
    val currentOpenPhoto by rememberUpdatedState(onOpenPhoto)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ShopMenuGalleryEvent.Back -> currentBack()
                is ShopMenuGalleryEvent.OpenPhoto -> currentOpenPhoto(event.imageUrls, event.initialIndex)
            }
        }
    }
    ShopMenuGalleryScreenContent(state, viewModel::onAction)
}

@Composable
internal fun ShopMenuGalleryScreenContent(
    state: ShopMenuGalleryState,
    onAction: (ShopMenuGalleryAction) -> Unit,
) {
    Scaffold(
        topBar = {
            CpTopBar(
                title = stringResource(Res.string.shop_menu_gallery_title),
                backDescription = stringResource(Res.string.shop_menu_gallery_back),
                onBack = { onAction(ShopMenuGalleryAction.Back) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.hasError || state.photos.isEmpty() -> GalleryMessage(state.hasError,
                    onRetry = { onAction(ShopMenuGalleryAction.Load) })
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = CpDimens.spacing4,
                        top = CpDimens.spacing2,
                        end = CpDimens.spacing4,
                        bottom = CpDimens.spacing6,
                    ),
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
                ) {
                    if (state.shopTitle.isNotBlank()) {
                        item {
                            Text(state.shopTitle, style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    itemsIndexed(state.photos, key = { index, photo -> "${photo.id}-${photo.fullUrl}-$index" }) {
                        index, photo ->
                        MenuPhotoCard(photo, index, state.photos.size,
                            onClick = { onAction(ShopMenuGalleryAction.OpenPhoto(index)) })
                    }
                }
            }
        }
    }
}

@Preview @Composable private fun ShopMenuGalleryScreenLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopMenuGalleryScreenContent(ShopMenuGalleryState(shopTitle = "Кофейня", isLoading = false,
        photos = listOf(MenuPhoto("1", ""), MenuPhoto("2", ""))), {})
}

@Preview @Composable private fun ShopMenuGalleryScreenDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopMenuGalleryScreenContent(ShopMenuGalleryState(shopTitle = "Кофейня", isLoading = false,
        photos = listOf(MenuPhoto("1", ""), MenuPhoto("2", ""))), {})
}

@Preview @Composable private fun ShopMenuGalleryScreenErrorPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopMenuGalleryScreenContent(ShopMenuGalleryState(isLoading = false, hasError = true), {})
}
