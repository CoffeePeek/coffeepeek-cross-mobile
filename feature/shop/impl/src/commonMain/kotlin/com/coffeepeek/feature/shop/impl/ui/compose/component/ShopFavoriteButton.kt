package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.feature.shop.api.ShopExposure
import com.coffeepeek.feature.shop.impl.ui.data.shopExposure
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_add
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_remove
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_unavailable
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_updating
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopFavoriteButton(
    isFavorite: Boolean,
    available: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val description = stringResource(when {
        !available -> Res.string.shop_favorite_unavailable
        isFavorite -> Res.string.shop_favorite_remove
        else -> Res.string.shop_favorite_add
    })
    CoffeePeekGlassIconButton(onClick = onClick, enabled = available && !isLoading,
        contentDescription = description, modifier = modifier.shopExposure(ShopExposure.Favorite), hazeState = hazeState) {
        if (isLoading) {
            CoffeePeekLoader(contentDescription = stringResource(Res.string.shop_favorite_updating),
                size = 18.dp, strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = if (isFavorite) CoffeePeekIcons.FavoriteFilled else CoffeePeekIcons.Favorite,
                contentDescription = null,
                tint = if (isFavorite) CoffeePeekColors.Error
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

@Preview @Composable private fun ShopFavoriteLoadingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopFavoriteButton(false, true, true, onClick = {})
}

@Preview @Composable private fun ShopFavoriteLoadingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopFavoriteButton(true, true, true, onClick = {})
}
