package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_add
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_remove
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_unavailable
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopFavoriteButton(
    isFavorite: Boolean,
    available: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(when {
        !available -> Res.string.shop_favorite_unavailable
        isFavorite -> Res.string.shop_favorite_remove
        else -> Res.string.shop_favorite_add
    })
    IconButton(onClick = onClick, enabled = available && !isLoading, modifier = modifier) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = if (isFavorite) CpIcons.FavoriteFilled else CpIcons.Favorite,
                contentDescription = description,
                tint = if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview @Composable private fun ShopFavoriteButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopFavoriteButton(false, true, false, onClick = {})
}

@Preview @Composable private fun ShopFavoriteButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopFavoriteButton(true, true, false, onClick = {})
}
