package com.coffeepeek.admin.ui.screen.shop

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.FullScreenImageDialog
import com.coffeepeek.admin.utils.PrefetchListPhotos
import org.koin.core.parameter.parametersOf

@Composable
fun ShopMenuGalleryScreen(shopId: String) {
    val localizedShopMenuGalleryOpen = stringResource(Res.string.shop_menu_gallery_open)
    val localizedLegacyShopMenuGalleryScreenShopMenuGalleryEmpty = stringResource(Res.string.legacy_shop_menu_gallery_screen_shop_menu_gallery_empty)

    val vm: ShopMenuGalleryViewModel = platformViewModel(parameters = { parametersOf(shopId) })
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    PrefetchListPhotos(listState, state.photos.mapIndexed { index, photo ->
        "${photo.id}-${photo.fullUrl}-$index" to listOf(photo.fullUrl)
    })
    var previewIndex by remember { mutableStateOf<Int?>(null) }

    previewIndex?.let { index ->
        FullScreenImageDialog(
            imageUrls = state.photos.map { it.fullUrl },
            initialIndex = index,
            onDismiss = { previewIndex = null },
        )
    }

    Scaffold(
        topBar = { CpTopBar(title = localizedShopMenuGalleryOpen) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> CoffeePeekLoader()
                state.error != null -> GalleryError(
                    message = state.error.orEmpty(),
                    onRetry = vm::load,
                )
                state.photos.isEmpty() -> Text(
                    text = localizedLegacyShopMenuGalleryScreenShopMenuGalleryEmpty,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = coffeePeekScrollContentPadding(PaddingValues(
                        start = CpDimens.spacing4,
                        top = CpDimens.spacing2,
                        end = CpDimens.spacing4,
                        bottom = CpDimens.spacing6,
                    )),
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
                ) {
                    if (state.shopTitle.isNotBlank()) {
                        item {
                            Text(
                                text = state.shopTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    itemsIndexed(
                        items = state.photos,
                        key = { index, photo -> "${photo.id}-${photo.fullUrl}-$index" },
                    ) { index, photo ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .clip(RoundedCornerShape(CpDimens.radiusLg))
                                .clickable { previewIndex = index },
                        ) {
                            CoffeeShopImage(
                                imageUrl = photo.fullUrl,
                                contentDescription = stringResource(Res.string.legacy_menu_photo_number, index + 1),
                                contentScale = ContentScale.Fit,
                                placeholderLabelSize = 18.sp,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.Black.copy(alpha = 0.48f),
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(CpDimens.spacing3),
                            ) {
                                Text(
                                    text = "${index + 1} / ${state.photos.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.padding(
                                        horizontal = CpDimens.spacing3,
                                        vertical = CpDimens.spacing1,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryError(
    message: String,
    onRetry: () -> Unit,
) {
    val localizedHistoryHomeRetry = stringResource(Res.string.history_home_retry)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        modifier = Modifier.padding(CpDimens.spacing4),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.height(CpDimens.buttonHeight),
            shape = RoundedCornerShape(percent = 50),
        ) {
            Text(localizedHistoryHomeRetry)
        }
    }
}
