package com.coffeepeek.feature.discovery.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.api.MapShopDetails
import com.coffeepeek.feature.discovery.api.MapOpeningHours
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapMarker
import com.coffeepeek.feature.discovery.impl.resources.*
import com.coffeepeek.feature.discovery.impl.ui.distanceLabel
import com.coffeepeek.feature.discovery.impl.ui.oneDecimal
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun DiscoveryMapShopCard(shop: MapMarker, details: MapShopDetails?, isLoading: Boolean,
    distance: Double?, onOpen: () -> Unit) {
    CoffeePeekCard(onClick = onOpen, shape = RoundedCornerShape(CoffeePeekDimens.cardRadius), elevation = 8.dp,
        contentPadding = PaddingValues(CoffeePeekDimens.spacing3)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MapShopPhoto(shop, details?.photoUrl, isLoading)
            Spacer(Modifier.width(CoffeePeekDimens.spacing3))
            MapShopSummary(shop, details, isLoading, distance, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MapShopPhoto(shop: MapMarker, photo: String?, isLoading: Boolean) {
    Box(Modifier.size(72.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
        .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        when {
            !photo.isNullOrBlank() -> CoffeePeekBrandedImage(photo, Modifier.fillMaxSize(), shop.title, placeholderLabelSize = 7.sp)
            isLoading -> CoffeePeekLoader(stringResource(Res.string.discovery_loading), size = 20.dp, strokeWidth = 2.dp)
            else -> CoffeePeekImagePlaceholder(Modifier.fillMaxSize(), labelSize = 7.sp,
                contentDescription = stringResource(Res.string.discovery_map_photo_missing, shop.title))
        }
    }
}

@Composable
private fun MapShopSummary(shop: MapMarker, details: MapShopDetails?, isLoading: Boolean, distance: Double?, modifier: Modifier) {
    Column(modifier) {
        MapShopRating(details)
        Text(shop.title, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        distance?.let { Text(distanceLabel(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        MapShopHours(details, isLoading)
    }
}

@Composable
private fun MapShopRating(details: MapShopDetails?) {
    val rating = details?.rating
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (rating != null && rating > 0) {
            Icon(CoffeePeekIcons.StarFilled, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(oneDecimal(rating), style = MaterialTheme.typography.labelMedium)
            if (details.reviewCount > 0) Text(" (${details.reviewCount})", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Text(when {
            details == null -> stringResource(Res.string.discovery_map_card_nearby)
            details.reviewCount > 0 -> stringResource(Res.string.discovery_map_card_checkins, details.reviewCount)
            else -> stringResource(Res.string.discovery_map_card_no_checkins)
        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MapShopHours(details: MapShopDetails?, isLoading: Boolean) {
    val summary = details?.hours
    val hours = if (summary?.isClosed == true) stringResource(Res.string.discovery_map_card_closed) else summary?.intervalLabel()
    when {
        hours != null -> Text(stringResource(Res.string.discovery_map_card_hours,
            stringResource(if (details?.isOpen == true) Res.string.discovery_map_card_open else Res.string.discovery_map_card_closed), hours),
            Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        isLoading -> Text(stringResource(Res.string.discovery_map_card_loading), Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun MapOpeningHours.intervalLabel(): String? {
    if (isClosed) return null
    val open = openTime?.split(":")?.take(2)?.joinToString(":") ?: return null
    val close = closeTime?.split(":")?.take(2)?.joinToString(":") ?: return null
    return "$open – $close"
}

@Composable private fun MapShopCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    DiscoveryMapShopCard(MapMarker("shop", "Кофейня", Coordinates(53.9, 27.5)),
        MapShopDetails(null, 4.7, 3, true, MapOpeningHours(false, "09:00", "23:00")), false, 350.0, {})
}
@Preview @Composable private fun DiscoveryMapShopCardLightPreview() = MapShopCardPreview(false)
@Preview @Composable private fun DiscoveryMapShopCardDarkPreview() = MapShopCardPreview(true)
