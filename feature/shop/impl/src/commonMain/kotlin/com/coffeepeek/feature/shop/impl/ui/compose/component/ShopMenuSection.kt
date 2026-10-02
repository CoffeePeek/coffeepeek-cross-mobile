package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopMenu
import com.coffeepeek.feature.shop.domain.model.ShopMenuItem
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_menu_captured
import com.coffeepeek.feature.shop.impl.resources.shop_menu_no_items
import com.coffeepeek.feature.shop.impl.resources.shop_menu_espresso
import com.coffeepeek.feature.shop.impl.resources.shop_menu_filter
import com.coffeepeek.feature.shop.impl.resources.shop_menu_other
import com.coffeepeek.feature.shop.impl.resources.shop_menu_photos
import com.coffeepeek.feature.shop.impl.resources.shop_menu_title
import com.coffeepeek.feature.shop.impl.resources.shop_menu_updated
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
internal fun ShopMenuSection(
    menu: ShopMenu,
    onOpenPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = groupedPresentItems(menu.items)
    Card(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            Text(stringResource(Res.string.shop_menu_title), style = MaterialTheme.typography.titleMedium)
            if (groups.isEmpty()) {
                Text(
                    stringResource(Res.string.shop_menu_no_items),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                groups.forEach { (category, items) ->
                    Text(
                        menuCategoryTitle(category),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    items.forEach { item -> MenuItemRow(item) }
                }
            }
            if (menu.photos.isNotEmpty()) {
                Button(onClick = onOpenPhotos) {
                    Text(stringResource(Res.string.shop_menu_photos, menu.photos.size))
                }
            }
            menu.capturedAtUtc?.takeIf(String::isNotBlank)?.let { captured ->
                Text(stringResource(Res.string.shop_menu_captured, captured.take(10)),
                    style = MaterialTheme.typography.labelSmall)
            }
            menu.updatedAtUtc?.takeIf { it.isNotBlank() && it != menu.capturedAtUtc }?.let { updated ->
                Text(stringResource(Res.string.shop_menu_updated, updated.take(10)),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MenuItemRow(item: ShopMenuItem) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        Text(
            item.nameRu.ifBlank { item.nameEn.ifBlank { item.slug } },
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        item.price?.let { price ->
            Text("${formatAmount(price)} ${item.currency}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatAmount(amount: Double): String {
    val cents = (amount * 100).roundToLong()
    val whole = cents / 100
    val remainder = abs(cents % 100).toString().padStart(2, '0')
    return "$whole,$remainder"
}

private fun groupedPresentItems(items: List<ShopMenuItem>): List<Pair<String, List<ShopMenuItem>>> {
    val grouped = items.filter { it.availability.equals("Present", ignoreCase = true) }
        .groupBy(ShopMenuItem::category)
    val first = listOf("Espresso", "Filter").mapNotNull { category ->
        grouped[category]?.let { category to it }
    }
    return first + grouped.filterKeys { it != "Espresso" && it != "Filter" }.toList()
}

@Composable
private fun menuCategoryTitle(category: String): String = when (category) {
    "Espresso" -> stringResource(Res.string.shop_menu_espresso)
    "Filter" -> stringResource(Res.string.shop_menu_filter)
    "" -> stringResource(Res.string.shop_menu_other)
    else -> category
}

@Preview @Composable private fun ShopMenuSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopMenuSection(previewMenu(), onOpenPhotos = {})
}

@Preview @Composable private fun ShopMenuSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopMenuSection(previewMenu(), onOpenPhotos = {})
}

private fun previewMenu() = ShopMenu(
    capturedAtUtc = "2026-10-02T10:00:00Z",
    currency = "BYN",
    items = listOf(
        ShopMenuItem("flat-white", "Флэт уайт", "Flat white", "Coffee", "Present", 5.5, "BYN", 250),
        ShopMenuItem("filter", "Фильтр", "Filter", "Coffee", "Present", 6.0, "BYN", null),
    ),
)
