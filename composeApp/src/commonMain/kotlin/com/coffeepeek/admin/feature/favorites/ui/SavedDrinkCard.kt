package com.coffeepeek.admin.feature.favorites.ui

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CoffeeShopImage
import com.coffeepeek.admin.ui.component.FavoriteButton
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry

/** Drinks have a favorite slug, but no contracted detail route. */
@Composable
internal fun SavedDrinkCard(drink: FavoriteEntry, removing: Boolean, onRemove: () -> Unit) {
    val localizedSelectDrinkLabel = stringResource(Res.string.select_drink_label)

    Card(
        shape = RoundedCornerShape(CpDimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(CpDimens.spacing3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(CpDimens.radiusMd))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                drink.coverPhotoUrl?.let { url -> CoffeeShopImage(
                    imageUrl = url, contentDescription = drink.name,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                ) } ?: Icon(CpIcons.Coffee, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                Text(drink.name.ifBlank { localizedSelectDrinkLabel }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(localizedSelectDrinkLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FavoriteButton(isFavorite = true, onClick = onRemove, enabled = !removing && !drink.drinkSlug.isNullOrBlank())
        }
    }
}
