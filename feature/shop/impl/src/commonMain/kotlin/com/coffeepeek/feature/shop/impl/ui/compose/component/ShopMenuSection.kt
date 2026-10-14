package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopMenu
import com.coffeepeek.feature.shop.domain.model.ShopMenuItem
import com.coffeepeek.feature.shop.domain.model.MenuPhoto
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_menu_no_items
import com.coffeepeek.feature.shop.impl.resources.shop_menu_photos
import com.coffeepeek.feature.shop.impl.resources.shop_menu_title
import com.coffeepeek.feature.shop.impl.ui.data.formatShopMenuDate
import com.coffeepeek.feature.shop.impl.ui.data.groupedPresentMenuItems
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopMenuSection(
    menu: ShopMenu,
    onOpenPhotos: () -> Unit,
    onOpenPhoto: (Int) -> Unit,
    modifier: Modifier = Modifier,
    formatDate: (String) -> String = ::formatShopMenuDate,
) {
    val groups = groupedPresentMenuItems(menu.items)
    val captured = menu.capturedAtUtc?.takeIf(String::isNotBlank)?.let(formatDate)
    val updated = menu.updatedAtUtc?.takeIf(String::isNotBlank)?.let(formatDate)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ShopSectionTitle(stringResource(Res.string.shop_menu_title), Modifier.weight(1f))
            if (menu.photos.isNotEmpty()) TextButton(onClick = onOpenPhotos) {
                Text(stringResource(Res.string.shop_menu_photos, menu.photos.size))
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(Modifier.padding(CoffeePeekDimens.spacing4),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    // Preserve the compact layout without squeezing text at large font scales.
                    val canPlaceBesideItems = maxWidth >= 280.dp && LocalDensity.current.fontScale <= 1.3f
                    val photoStack: @Composable () -> Unit = {
                        ShopMenuPhotoStack(menu.photos, onOpenPhoto) {
                            ShopMenuFreshness(captured, updated,
                                Modifier.align(Alignment.BottomStart).padding(CoffeePeekDimens.spacing2),
                                contentColor = Color.White)
                        }
                    }
                    if (menu.photos.isNotEmpty() && groups.isNotEmpty() && canPlaceBesideItems) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
                            verticalAlignment = Alignment.Top) {
                            MenuItems(groups, Modifier.weight(1f))
                            photoStack()
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                            if (groups.isNotEmpty()) MenuItems(groups)
                            else if (menu.photos.isEmpty()) Text(stringResource(Res.string.shop_menu_no_items),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (menu.photos.isNotEmpty()) photoStack()
                        }
                    }
                }
                if (menu.photos.isEmpty()) ShopMenuFreshness(captured, updated)
            }
        }
    }
}

@Composable
private fun MenuItems(groups: List<List<ShopMenuItem>>, modifier: Modifier = Modifier) {
    Column(modifier) {
        groups.forEachIndexed { index, items ->
            if (index > 0) HorizontalDivider(Modifier.padding(vertical = CoffeePeekDimens.spacing2),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            items.forEach { ShopMenuDrinkRow(it) }
        }
    }
}

@Preview @Composable private fun ShopMenuSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopMenuSection(previewMenu(), onOpenPhotos = {}, onOpenPhoto = {})
}
@Preview @Composable private fun ShopMenuSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopMenuSection(previewMenu(), onOpenPhotos = {}, onOpenPhoto = {})
}
@Preview @Composable private fun ShopMenuWithPhotosLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopMenuSection(previewMenu().copy(photos = List(3) { MenuPhoto("preview-$it", "", "") }), {}, {})
}
@Preview @Composable private fun ShopMenuWithPhotosDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopMenuSection(previewMenu().copy(photos = List(3) { MenuPhoto("preview-$it", "", "") }), {}, {})
}
private fun previewMenu() = ShopMenu(
    capturedAtUtc = "2026-10-02T10:00:00Z",
    updatedAtUtc = "2026-10-03T10:00:00Z",
    currency = "BYN",
    items = listOf(
        ShopMenuItem("flat-white", "Флэт уайт", "Flat white", "Espresso", "Present", 5.5, "BYN", 250),
        ShopMenuItem("filter", "Фильтр", "Filter", "Filter", "Present", 6.0, "BYN", null),
    ),
)
