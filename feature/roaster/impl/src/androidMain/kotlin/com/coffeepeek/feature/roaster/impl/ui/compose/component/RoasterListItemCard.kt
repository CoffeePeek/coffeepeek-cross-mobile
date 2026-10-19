package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.roaster.impl.ui.compose.RoasterImage
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListAction
import com.coffeepeek.feature.roaster.impl.ui.model.RoasterListItem

/** A card bound to list actions; requests its details once the list settled, like the legacy list. */
@Composable
internal fun RoasterListItemCard(
    item: RoasterListItem,
    isLoading: Boolean,
    isFavorite: Boolean,
    onAction: (RoasterListAction) -> Unit,
    onFavoriteFeedback: () -> Unit,
    image: RoasterImage,
    modifier: Modifier = Modifier,
) {
    val slug = item.slug
    LaunchedEffect(slug, isLoading) { if (slug != null && !isLoading) onAction(RoasterListAction.LoadDetails(slug)) }
    RoasterCard(
        name = item.summary.name,
        photoUrl = item.summary.photoUrl ?: item.details?.photos?.firstOrNull()?.url,
        description = item.details?.about,
        shopsCount = item.summary.coffeeShopsCount,
        productsCount = item.summary.coffeeProductsCount,
        isFavorite = isFavorite,
        onOpen = slug?.let { { onAction(RoasterListAction.OpenRoaster(it)) } },
        onToggleFavorite = slug?.let { { onFavoriteFeedback(); onAction(RoasterListAction.ToggleFavorite(it)) } },
        image = image,
        modifier = modifier,
    )
}
