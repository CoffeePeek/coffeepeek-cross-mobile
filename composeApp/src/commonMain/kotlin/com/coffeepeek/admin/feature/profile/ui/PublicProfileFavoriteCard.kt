package com.coffeepeek.admin.feature.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.feature.catalog.api.RoasterCard
import com.coffeepeek.admin.feature.catalog.api.ShopCard
import com.coffeepeek.admin.feature.profile.domain.PublicFavoriteKind
import com.coffeepeek.admin.feature.profile.domain.PublicProfileFavorite
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.domain.model.CoffeeShop

/** Reuses catalog's public card entry points with the viewed user's favorite state. */
@Composable
internal fun PublicProfileFavoriteCard(
    favorite: PublicProfileFavorite,
    modifier: Modifier = Modifier.width(300.dp),
    onClick: () -> Unit,
) {
    val address = favorite.publicAddress
    when (favorite.kind) {
        PublicFavoriteKind.CoffeeShop -> ShopCard(
            shop = CoffeeShop(
                id = favorite.slug ?: favorite.id,
                publicAddress = address,
                title = favorite.title,
                rating = favorite.rating,
                reviewCount = favorite.reviewCount,
                cityName = null,
                priceRange = null,
                photoUrl = favorite.imageUrl,
                address = favorite.address,
                isOpen = favorite.isOpen == true,
                isFavorite = true,
                tags = favorite.tags,
            ),
            onClick = onClick,
            onToggleFavorite = null,
            modifier = modifier,
            showCatalogDetails = false,
            showOpenStatus = favorite.isOpen != null,
        )
        PublicFavoriteKind.Roaster -> RoasterCard(
            roaster = CatalogItem(
                id = favorite.slug ?: favorite.id,
                slug = favorite.slug.orEmpty(),
                name = favorite.title,
                photoUrl = favorite.imageUrl,
                address = address,
                description = favorite.description,
                coffeeShopsCount = favorite.coffeeShopsCount ?: 0,
                coffeeProductsCount = favorite.coffeeProductsCount ?: 0,
                availableCoffeeProducts = favorite.availableCoffeeProducts ?: 0,
            ),
            onClick = onClick,
            isFavorite = true,
            isFavoriteLoading = false,
            onToggleFavorite = null,
            modifier = modifier,
        )
        PublicFavoriteKind.Drink -> PublicDrinkFavoriteCard(favorite, modifier)
    }
}

@Composable
private fun PublicDrinkFavoriteCard(favorite: PublicProfileFavorite, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(CpDimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(2f).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!favorite.imageUrl.isNullOrBlank()) CoffeeShopImage(
                imageUrl = favorite.imageUrl, contentDescription = favorite.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            ) else Icon(
                CpIcons.Coffee, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp),
            )
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(CpDimens.spacing3)
                    .size(40.dp).clip(CircleShape).background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CpIcons.FavoriteFilled, "В избранном", tint = CpColor.Primary, modifier = Modifier.size(24.dp))
            }
        }
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            Text(
                favorite.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text("Напиток", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
