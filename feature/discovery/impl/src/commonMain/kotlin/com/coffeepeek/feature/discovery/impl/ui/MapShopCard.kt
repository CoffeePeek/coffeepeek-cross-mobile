package com.coffeepeek.feature.discovery.impl.ui

import com.coffeepeek.feature.discovery.impl.resources.Res
import com.coffeepeek.feature.discovery.impl.resources.discovery_loading
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_checkins
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_closed
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_hours
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_loading
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_nearby
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_no_checkins
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_card_open
import com.coffeepeek.feature.discovery.impl.resources.discovery_map_photo_missing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.api.DiscoveryHost
import com.coffeepeek.feature.discovery.api.MapShopDetails
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.MapMarker

@Composable
internal fun MapShopCard(shop: MapMarker, host: DiscoveryHost, details: MapShopDetails?,
    isLoading: Boolean, distance: Double?, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(CoffeePeekDimens.spacing3),
            verticalAlignment = Alignment.CenterVertically) {
            MapShopPhoto(shop, host, details?.photoUrl, isLoading)
            Spacer(Modifier.width(CoffeePeekDimens.spacing3))
            Column(Modifier.weight(1f)) { MapShopInformation(shop.title, details, isLoading, distance) }
        }
    }
}

@Composable
private fun MapShopPhoto(shop: MapMarker, host: DiscoveryHost, photo: String?, isLoading: Boolean) {
    Box(Modifier.size(72.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
        .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        when {
            !photo.isNullOrBlank() -> host.compactImage(photo, shop.title, Modifier.fillMaxSize(), ContentScale.Crop)
            isLoading -> CoffeePeekLoader(stringResource(Res.string.discovery_loading), size = 20.dp, strokeWidth = 2.dp)
            else -> host.compactCoverPlaceholder(stringResource(Res.string.discovery_map_photo_missing, shop.title), Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun MapShopInformation(title: String, details: MapShopDetails?, isLoading: Boolean, distance: Double?) {
    MapShopRating(details)
    Text(title, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Text(distance?.let { distanceLabel(it) }.orEmpty(), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 1, maxLines = 1)
    val schedule = details?.hours
    val hours = when {
        schedule != null -> stringResource(Res.string.discovery_map_card_hours,
            stringResource(if (details?.isOpen == true) Res.string.discovery_map_card_open else Res.string.discovery_map_card_closed), schedule)
        isLoading -> stringResource(Res.string.discovery_map_card_loading)
        else -> ""
    }
    Text(hours, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun MapShopRating(details: MapShopDetails?) {
    val lineHeight = 20.sp
    val rowHeight = with(LocalDensity.current) { lineHeight.toDp().coerceAtLeast(20.dp) }
    Row(Modifier.height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
        val rating = details?.rating
        if (rating != null && rating > 0) {
            Icon(CoffeePeekIcons.StarFilled, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(oneDecimal(rating), style = MaterialTheme.typography.labelMedium, lineHeight = lineHeight,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            if (details.reviewCount > 0) Text(" (${details.reviewCount})", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = lineHeight,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        } else {
            Text(when {
                details == null -> stringResource(Res.string.discovery_map_card_nearby)
                details.reviewCount > 0 -> stringResource(Res.string.discovery_map_card_checkins, details.reviewCount)
                else -> stringResource(Res.string.discovery_map_card_no_checkins)
            }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = lineHeight, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Preview
@Composable
private fun MapShopCardPreview() = CoffeePeekTheme {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val shop = MapMarker("preview", "CoffeePeek Specialty Coffee", Coordinates(53.9, 27.56))
        MapShopCard(shop, DiscoveryHost(), MapShopDetails(null, 4.5, 12, true, "09:00 – 21:00"), false, 250.0, {})
        MapShopCard(shop.copy(title = "Cafe"), DiscoveryHost(), null, true, null, {})
    }
}
