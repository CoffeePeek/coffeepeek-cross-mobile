package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coffeepeek.core.designsystem.component.CoffeePeekBrandedImage
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.domain.rules.distanceMeters
import org.jetbrains.compose.ui.tooling.preview.Preview

/** A choice of place, with only the details needed to identify its branch. */
@Composable
internal fun CheckInShopPickerRow(shop: ShopSummary, location: Coordinates?, onClick: () -> Unit) {
    val distance = shop.distanceMeters ?: location?.let { user ->
        shop.coordinates?.let { distanceMeters(user, it) }
    }
    CoffeePeekCard(onClick = onClick, elevation = 0.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(CoffeePeekDimens.spacing3)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            val photo = shop.photoUrl
            if (photo.isNullOrBlank()) CoffeePeekIconBadge(CoffeePeekIcons.Location,
                CoffeePeekIconBadgePalette.Gold, Modifier.size(56.dp))
            else CoffeePeekBrandedImage(photo, Modifier.size(56.dp)
                .clip(RoundedCornerShape(CoffeePeekDimens.radiusSm)), shop.title)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(shop.title, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                shop.address?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                distance?.let {
                    Text(distanceLabel(it), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview @Composable private fun CheckInShopPickerRowLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInShopPickerRow(ShopSummary("shop", "The Coffee Room", address = "ул. Петра Мстиславца, 20",
        distanceMeters = 850.0), null, {})
}

@Preview @Composable private fun CheckInShopPickerRowDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInShopPickerRow(ShopSummary("shop", "The Coffee Room", address = "ул. Петра Мстиславца, 20",
        distanceMeters = 850.0), null, {})
}
