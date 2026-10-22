package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.component.CoffeePeekShopCard
import com.coffeepeek.feature.discovery.api.DiscoveryExposure
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.impl.R
import java.util.Locale

@Composable
internal fun ShopRow(shop: ShopSummary, host: DiscoveryHost, favoritePending: Boolean, onOpen: () -> Unit, onFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val kind = stringResource(
        when (shop.kind) {
            ShopKind.Specialty -> R.string.discovery_shop_kind_specialty
            ShopKind.CoffeeBar -> R.string.discovery_shop_kind_coffee_bar
            ShopKind.Cafe -> R.string.discovery_shop_kind_cafe
        },
    )
    val distance = shop.distanceMeters?.let { meters ->
        if (meters < 1000) stringResource(R.string.discovery_distance_meters, meters.toInt())
        else stringResource(R.string.discovery_distance_km, String.format(Locale.getDefault(), "%.1f", meters / 1000))
    }
    CoffeePeekShopCard(
        title = shop.title,
        kindLabel = kind,
        modifier = host.exposure(modifier, DiscoveryExposure.ShopCatalog),
        rating = shop.rating,
        ratingText = shop.rating?.let { String.format(Locale.getDefault(), "%.1f", it) }.orEmpty(),
        ratingCount = shop.checkInCount,
        isNew = shop.isNew,
        newLabel = stringResource(R.string.discovery_shop_new),
        statusLabel = if (shop.isOpen) stringResource(R.string.discovery_shop_open) else null,
        isOpen = shop.isOpen,
        locationSummary = listOfNotNull(shop.address?.takeIf { it.isNotBlank() }, distance).joinToString(" · ").ifEmpty { null },
        priceLevel = shop.priceLevel?.level,
        tag = shop.tags.firstOrNull(),
        onClick = onOpen,
        cover = {
            if (shop.photoUrl != null) host.image(shop.photoUrl!!, shop.title, Modifier.fillMaxSize(), ContentScale.Crop)
            else host.coverPlaceholder(shop.title, Modifier.fillMaxSize())
        },
        roasterLogoCount = shop.roasterLogos.size,
        roasterLogo = { index -> host.roasterLogo(shop.title, shop.roasterLogos[index]) },
        brewMethods = shop.brewMethods.takeIf { it.isNotEmpty() }?.let { methods ->
            { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { methods.forEach { host.brewMethodIcon(it) } } }
        },
        favorite = {
            CoffeePeekLikeButton(
                selected = shop.isFavorite,
                contentDescription = stringResource(if (shop.isFavorite) R.string.discovery_favorite_remove else R.string.discovery_favorite_add),
                onClick = { host.haptic(); onFavorite() },
                modifier = host.exposure(Modifier, DiscoveryExposure.Favorite),
                enabled = !favoritePending,
                overImage = true,
            )
        },
    )
}
