package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.component.CoffeePeekShopCard
import com.coffeepeek.core.designsystem.component.CoffeePeekShopInfoChip
import com.coffeepeek.feature.discovery.api.DiscoveryExposure
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.domain.rules.distanceMeters
import com.coffeepeek.feature.discovery.impl.R
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ShopRow(
    shop: ShopSummary,
    host: DiscoveryHost,
    favoritePending: Boolean,
    userLocation: Coordinates?,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    /** The roaster logos, the brew methods and the tag belong to the list card; the row of the discovery turns them off. */
    showCatalogDetails: Boolean = true,
) {
    val kind = stringResource(
        when (shop.kind) {
            ShopKind.Specialty -> R.string.discovery_shop_kind_specialty
            ShopKind.CoffeeBar -> R.string.discovery_shop_kind_coffee_bar
            ShopKind.Cafe -> R.string.discovery_shop_kind_cafe
        },
    )
    // The server's distance first, else measured from where the user is.
    val distance = (shop.distanceMeters ?: userLocation?.let { user -> shop.coordinates?.let { distanceMeters(user, it) } })?.let { stringResource(R.string.discovery_distance_from_you, distanceLabel(it)) }
    CoffeePeekShopCard(
        title = shop.title,
        kindLabel = kind,
        modifier = modifier,
        rating = shop.rating,
        ratingText = shop.rating?.let { String.format(Locale.getDefault(), "%.1f", it) }.orEmpty(),
        ratingCount = shop.checkInCount,
        isNew = shop.isNew,
        newLabel = stringResource(R.string.discovery_shop_new),
        statusLabel = if (shop.isOpen) stringResource(R.string.discovery_shop_open) else null,
        isOpen = shop.isOpen,
        locationSummary = listOfNotNull(shop.address?.takeIf { it.isNotBlank() }, distance).joinToString(" • ").ifEmpty { null },
        priceLevel = shop.priceLevel?.level,
        tag = shop.tags.takeIf { showCatalogDetails }.orEmpty().firstOrNull { it !in shop.brewMethods },
        onClick = onOpen,
        cover = {
            if (shop.photoUrl != null) host.image(shop.photoUrl!!, shop.title, Modifier.fillMaxSize(), ContentScale.Crop)
            else host.coverPlaceholder(shop.title, Modifier.fillMaxSize())
        },
        roasterLogoCount = if (showCatalogDetails) shop.roasterLogos.size else 0,
        roasterLogo = { index -> host.roasterLogo(shop.title, shop.roasterLogos[index]) },
        brewMethods = shop.brewMethods.takeIf { showCatalogDetails && it.isNotEmpty() }?.let { methods ->
            {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    methods.take(2).forEach { method -> CoffeePeekShopInfoChip(method, Modifier.widthIn(max = 112.dp), icon = { host.brewMethodIcon(method, 13.dp) }) }
                    if (methods.size > 2) CoffeePeekShopInfoChip("+${methods.size - 2}")
                }
            }
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

/** «350 м», «1,2 км»: the legacy wording, tenths of a kilometer with a comma whatever the locale. */
@Composable
internal fun distanceLabel(meters: Double): String {
    if (meters < 1000) return stringResource(R.string.discovery_distance_meters, meters.roundToInt())
    val tenths = (meters / 100).roundToInt()
    val text = if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10},${tenths % 10}"
    return stringResource(R.string.discovery_distance_km, text)
}
