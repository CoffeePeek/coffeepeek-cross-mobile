package com.coffeepeek.feature.shop.impl.ui.compose.component

import com.coffeepeek.feature.shop.api.ShopExposure
import com.coffeepeek.feature.shop.impl.ui.data.shopExposure
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButtonStyle
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_add
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_remove
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_unavailable
import com.coffeepeek.feature.shop.impl.resources.shop_favorite_updating
import dev.chrisbanes.haze.HazeState
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
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
    CompositionLocalProvider(LocalCoffeePeekGlassHazeState provides hazeState) {
        CoffeePeekLikeButton(isFavorite, description, onClick, modifier.shopExposure(ShopExposure.Favorite),
            style = CoffeePeekLikeButtonStyle.FloatingGlass, isLoading = isLoading,
            loadingDescription = stringResource(Res.string.shop_favorite_updating))
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

@Preview @Composable private fun ShopFavoriteUnavailableLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopFavoriteButton(false, false, false, onClick = {})
}

@Preview @Composable private fun ShopFavoriteUnavailableDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopFavoriteButton(false, false, false, onClick = {})
}
