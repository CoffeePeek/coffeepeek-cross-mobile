package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.favorites.domain.model.FavoriteDrink
import com.coffeepeek.feature.favorites.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Saved drinks intentionally have no invented detail route. */
@Composable
internal fun FavoriteDrinkCard(drink: FavoriteDrink, removing: Boolean, onRemove: () -> Unit) {
    val title = drink.title.ifBlank { stringResource(Res.string.favorites_drink) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                if (drink.photoUrl == null) Icon(CoffeePeekIcons.Coffee, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                else FavoritePhoto(drink.photoUrl, title, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(Res.string.favorites_drink), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconToggleButton(true, { onRemove() }, enabled = !removing && drink.canRemove,
                colors = IconButtonDefaults.iconToggleButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    checkedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    checkedContentColor = MaterialTheme.colorScheme.onSurface)) {
                Icon(CoffeePeekIcons.FavoriteFilled, stringResource(Res.string.favorites_remove_description, title))
            }
        }
    }
}

@Preview @Composable private fun FavoriteDrinkLightPreview() = CoffeePeekTheme(darkTheme = false) {
    FavoriteDrinkCard(FavoriteDrink("sample", "Капучино", null, true), false, {})
}
@Preview @Composable private fun FavoriteDrinkDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    FavoriteDrinkCard(FavoriteDrink("sample", "Капучино", null, true), false, {})
}
