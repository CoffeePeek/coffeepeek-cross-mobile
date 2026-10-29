package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.core.designsystem.component.CoffeePeekBrandedImage
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.feature.discovery.impl.ui.compose.component.DiscoveryBrewMethodIcon
import com.coffeepeek.feature.discovery.impl.ui.compose.component.DiscoveryShopRoasterLogo

import androidx.compose.foundation.layout.Arrangement
import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_distance_from_you
import com.coffeepeek.feature.discovery.impl.resources.discovery_distance_km
import com.coffeepeek.feature.discovery.impl.resources.discovery_distance_meters
import com.coffeepeek.feature.discovery.impl.resources.discovery_favorite_add
import com.coffeepeek.feature.discovery.impl.resources.discovery_favorite_remove
import com.coffeepeek.feature.discovery.impl.resources.discovery_shop_closed
import com.coffeepeek.feature.discovery.impl.resources.discovery_shop_kind_cafe
import com.coffeepeek.feature.discovery.impl.resources.discovery_shop_kind_coffee_bar
import com.coffeepeek.feature.discovery.impl.resources.discovery_shop_kind_specialty
import com.coffeepeek.feature.discovery.impl.resources.discovery_shop_new
import com.coffeepeek.feature.discovery.impl.resources.discovery_shop_open
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
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
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
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
    // The server's distance first, else measured from where the user is.
    val distance = (shop.distanceMeters ?: userLocation?.let { user -> shop.coordinates?.let { distanceMeters(user, it) } })?.let { stringResource(Res.string.discovery_distance_from_you, distanceLabel(it)) }
    CoffeePeekShopCard(
        title = shop.title,
        kindLabel = shopKindLabel(shop.kind),
        modifier = modifier,
        rating = shop.rating,
        ratingText = shop.rating?.let(::oneDecimal).orEmpty(),
        ratingCount = shop.checkInCount,
        isNew = shop.isNew,
        newLabel = stringResource(Res.string.discovery_shop_new),
        statusLabel = stringResource(if (shop.isOpen) Res.string.discovery_shop_open else Res.string.discovery_shop_closed),
        isOpen = shop.isOpen,
        locationSummary = listOfNotNull(shop.address?.takeIf { it.isNotBlank() }, distance).joinToString(" • ").ifEmpty { null },
        priceLevel = shop.priceLevel?.level,
        tag = shop.tags.takeIf { showCatalogDetails }.orEmpty().firstOrNull { it !in shop.brewMethods },
        onClick = onOpen,
        cover = {
            if (shop.photoUrl != null) CoffeePeekBrandedImage(shop.photoUrl!!, Modifier.fillMaxSize(), shop.title)
            else CoffeePeekImagePlaceholder(Modifier.fillMaxSize(), contentDescription = shop.title)
        },
        roasterLogoCount = if (showCatalogDetails) shop.roasterLogos.size else 0,
        roasterLogo = { index -> DiscoveryShopRoasterLogo(shop.title, shop.roasterLogos[index]) },
        brewMethods = shop.brewMethods.takeIf { showCatalogDetails && it.isNotEmpty() }?.let { methods ->
            {
                ShopBrewMethods(methods)
            }
        },
        favorite = {
            ShopFavorite(shop.isFavorite, !favoritePending, host, onFavorite)
        },
    )
}

@Composable
private fun shopKindLabel(kind: ShopKind): String = stringResource(when (kind) {
    ShopKind.Specialty -> Res.string.discovery_shop_kind_specialty
    ShopKind.CoffeeBar -> Res.string.discovery_shop_kind_coffee_bar
    ShopKind.Cafe -> Res.string.discovery_shop_kind_cafe
})

@Composable
private fun ShopBrewMethods(methods: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        methods.take(2).forEach { method ->
            CoffeePeekShopInfoChip(method, Modifier.widthIn(max = 112.dp), icon = { DiscoveryBrewMethodIcon(method, 13.dp) })
        }
        if (methods.size > 2) CoffeePeekShopInfoChip("+${methods.size - 2}")
    }
}

@Composable
private fun ShopFavorite(selected: Boolean, enabled: Boolean, host: DiscoveryHost, onFavorite: () -> Unit) {
    CoffeePeekLikeButton(selected, contentDescription = stringResource(if (selected)
        Res.string.discovery_favorite_remove else Res.string.discovery_favorite_add),
        onClick = {
            host.haptic()
            onFavorite()
        }, modifier = host.exposure(Modifier, DiscoveryExposure.Favorite), enabled = enabled, overImage = true)
}

/** «350 м», «1,2 км»: the legacy wording, tenths of a kilometer with a comma whatever the locale. */
@Composable
internal fun distanceLabel(meters: Double): String {
    if (meters < 1000) return stringResource(Res.string.discovery_distance_meters, meters.roundToInt())
    val tenths = (meters / 100).roundToInt()
    val text = if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10},${tenths % 10}"
    return stringResource(Res.string.discovery_distance_km, text)
}

/** One decimal with a dot in every locale, as in prod (the rating badge never shows a decimal comma). */
internal fun oneDecimal(value: Double): String {
    val tenths = kotlin.math.round(value * 10.0).toLong()
    val absolute = kotlin.math.abs(tenths)
    return "${if (tenths < 0) "-" else ""}${absolute / 10}.${absolute % 10}"
}

@Composable private fun ShopRowPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShopRow(ShopSummary("shop", "Кофейня", rating = 4.7, checkInCount = 12, brewMethods = listOf("V60", "Эспрессо")),
        DiscoveryHost(), false, null, {}, {})
}
@Preview @Composable private fun ShopRowLightPreview() = ShopRowPreview(false)
@Preview @Composable private fun ShopRowDarkPreview() = ShopRowPreview(true)
