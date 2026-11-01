package com.coffeepeek.admin.ui.screen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.utils.formatOneDecimal
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.CoffeeShop
import com.coffeepeek.domain.model.CoffeeShopDetails
import com.coffeepeek.domain.model.MapShop
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun MapShopCard(shop: MapShop, details: CoffeeShopDetails?, isLoadingDetails: Boolean,
    onOpen: () -> Unit, distance: String?, modifier: Modifier = Modifier) {
    CoffeePeekCard(modifier, onClick = onOpen, shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        elevation = 8.dp, contentPadding = PaddingValues(CoffeePeekDimens.spacing2)) {
        Row(Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            MapCardPhoto(shop.title, details?.shop?.photoUrl ?: details?.photos?.firstOrNull(), isLoadingDetails)
            Spacer(Modifier.width(CoffeePeekDimens.spacing2))
            Column(Modifier.weight(1f)) { MapCardInformation(shop.title, details, isLoadingDetails, distance) }
        }
    }
}

@Composable
private fun MapCardInformation(title: String, details: CoffeeShopDetails?, loading: Boolean, distance: String?) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        maxLines = 2, overflow = TextOverflow.Ellipsis)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MapCardRating(details)
        if (distance != null) Text(distance, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
    MapCardHours(details, loading)
}

@Composable
private fun MapCardPhoto(title: String, photoUrl: String?, loading: Boolean) {
    Box(Modifier.size(64.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center) {
        when {
            !photoUrl.isNullOrBlank() -> CoffeeShopImage(imageUrl = photoUrl, contentDescription = title,
                contentScale = ContentScale.Crop, placeholderLabelSize = 7.sp, modifier = Modifier.fillMaxSize())
            loading -> CoffeePeekLoader(stringResource(Res.string.discovery_map_card_loading), size = 20.dp)
            else -> CoffeePeekImagePlaceholder(labelSize = 7.sp, contentDescription = stringResource(Res.string.discovery_map_card_photo_missing, title))
        }
    }
}

@Composable
private fun MapCardRating(details: CoffeeShopDetails?) {
    val rating = details?.shop?.rating
    val reviews = details?.shop?.reviewCount ?: 0
    val rowHeight = maxOf(20.dp, with(LocalDensity.current) { 20.sp.toDp() })
    val medium = MaterialTheme.typography.labelMedium.copy(lineHeight = 20.sp)
    val small = MaterialTheme.typography.labelSmall.copy(lineHeight = 20.sp)
    Row(Modifier.height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
        if (rating != null && rating > 0) {
            Icon(CoffeePeekIcons.StarFilled, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(formatOneDecimal(rating), style = medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (reviews > 0) Text(" ($reviews)", style = small, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            val label = when {
                details == null -> stringResource(Res.string.discovery_map_card_nearby)
                reviews > 0 -> stringResource(Res.string.discovery_map_card_checkins, reviews)
                else -> stringResource(Res.string.discovery_map_card_no_checkins)
            }
            Text(label, style = small, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MapCardHours(details: CoffeeShopDetails?, loading: Boolean) {
    val hours = details?.schedules?.let(::formatMapHoursSummary)
    val status = stringResource(if (details?.shop?.isOpen == true) Res.string.discovery_map_card_open else Res.string.discovery_map_card_closed)
    val label = when {
        hours != null -> "$status · $hours"
        loading -> stringResource(Res.string.discovery_map_card_loading)
        else -> ""
    }
    if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun MapShopCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val shop = MapShop("preview", "CoffeePeek", 53.9, 27.56)
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MapShopCard(shop, null, true, {}, "150 м")
        MapShopCard(shop.copy(title = "CoffeePeek"), CoffeeShopDetails(CoffeeShop(id = "preview", title = "CoffeePeek", rating = 4.5,
            cityName = null, priceRange = null, photoUrl = null)), false, {}, "150 м")
        MapShopCard(shop.copy(title = "CoffeePeek Specialty Coffee on a Very Long Street"), null, false, {}, null)
    }
}

@Preview @Composable private fun MapShopCardLightPreview() = MapShopCardPreview(false)
@Preview @Composable private fun MapShopCardDarkPreview() = MapShopCardPreview(true)
