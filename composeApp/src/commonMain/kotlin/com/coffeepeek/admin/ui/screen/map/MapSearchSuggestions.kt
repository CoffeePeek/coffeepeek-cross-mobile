package com.coffeepeek.admin.ui.screen.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.CoffeeShop
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun MapSearchSuggestions(results: List<CoffeeShop>, isLoading: Boolean, failed: Boolean, onSelect: (CoffeeShop) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(CpDimens.radiusLg),
        color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
        when {
            isLoading -> Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(size = 24.dp)
            }
            failed || results.isEmpty() -> Text(if (failed) "Не удалось выполнить поиск" else "Кофейни не найдены",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(CpDimens.spacing4))
            else -> Column {
                results.forEachIndexed { index, shop ->
                    MapSearchSuggestion(shop, onSelect)
                    if (index != results.lastIndex) HorizontalDivider(
                        Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun MapSearchSuggestion(shop: CoffeeShop, onSelect: (CoffeeShop) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onSelect(shop) }
        .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
        Icon(CpIcons.Location, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(shop.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            shop.address?.takeIf(String::isNotBlank)?.let { address ->
                Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(CpIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun MapSearchSuggestionsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    MapSearchSuggestions(listOf(CoffeeShop("preview", "CoffeePeek", null, cityName = null, priceRange = null, photoUrl = null)), false, false, {})
}

@Preview @Composable private fun MapSearchSuggestionsLightPreview() = MapSearchSuggestionsPreview(false)
@Preview @Composable private fun MapSearchSuggestionsDarkPreview() = MapSearchSuggestionsPreview(true)
