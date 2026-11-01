package com.coffeepeek.admin.ui.screen.map

import com.coffeepeek.admin.ui.component.pluralCountWord

import com.coffeepeek.core.designsystem.resources.coffeePeekStringArrayResource as stringArrayResource

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.MapCoffeeZone
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun MapZoneCard(zone: MapCoffeeZone, onShowShops: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val localizedLegacyMapZoneCardDiscoveryMapZoneShowShops = stringResource(Res.string.legacy_map_zone_card_discovery_map_zone_show_shops)

    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(CpDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            MapZoneHeading(zone, onDismiss)
            if (zone.description.isNotBlank()) Text(zone.description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Button(onShowShops, Modifier.fillMaxWidth().height(CpDimens.buttonHeight), shape = RoundedCornerShape(percent = 50)) {
                Text(localizedLegacyMapZoneCardDiscoveryMapZoneShowShops)
            }
        }
    }
}

@Composable
private fun MapZoneHeading(zone: MapCoffeeZone, onDismiss: () -> Unit) {
    val localizedHistoryHomeClose = stringResource(Res.string.history_home_close)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(zone.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${zone.shopCount} ${mapShopCountLabel(zone.shopCount)}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onDismiss) { Icon(CpIcons.Close, contentDescription = localizedHistoryHomeClose) }
    }
}

@Composable
private fun mapShopCountLabel(count: Int): String = pluralCountWord(count, stringArrayResource(Res.array.legacy_shop_forms))

@Composable
private fun MapZoneCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    MapZoneCard(MapCoffeeZone("preview", "Центр", "Кофейни в пешей доступности",
        latitude = 53.9, longitude = 27.56, radiusMeters = 500.0, shopCount = 3), {}, {})
}

@Preview @Composable private fun MapZoneCardLightPreview() = MapZoneCardPreview(false)
@Preview @Composable private fun MapZoneCardDarkPreview() = MapZoneCardPreview(true)
