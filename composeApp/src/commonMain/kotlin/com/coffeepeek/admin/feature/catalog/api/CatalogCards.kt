package com.coffeepeek.admin.feature.catalog.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.feature.catalog.ui.RoasterCardContent
import com.coffeepeek.admin.feature.catalog.ui.ShopCardContent
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.RoasterDetails

/** Catalog presentation entry points shared by discovery and favorites. */
@Composable
internal fun ShopCard(
    shop: CoffeeShop,
    distance: String? = null,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    showCatalogDetails: Boolean = true,
) = ShopCardContent(shop, distance, onClick, onToggleFavorite, modifier, showCatalogDetails)

@Composable
internal fun RoasterCard(
    roaster: CatalogItem,
    details: RoasterDetails? = null,
    onClick: () -> Unit,
    isFavorite: Boolean,
    isFavoriteLoading: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) = RoasterCardContent(roaster, details, onClick, isFavorite, isFavoriteLoading, onToggleFavorite, modifier)
