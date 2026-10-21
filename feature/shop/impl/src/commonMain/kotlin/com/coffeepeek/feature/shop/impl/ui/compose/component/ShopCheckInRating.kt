package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_rating_description
import com.coffeepeek.feature.shop.impl.ui.data.formatShopRating
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInRating(rating: ShopRating, modifier: Modifier = Modifier) {
    val value = formatShopRating(rating.average)
    val description = stringResource(Res.string.shop_checkins_rating_description,
        rating.coffee, rating.service, rating.place, value)
    CoffeePeekRating(rating.average, modifier, valueText = value, contentDescription = description)
}

@Preview @Composable private fun ShopCheckInRatingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInRating(ShopRating(4, 5, 5))
}
@Preview @Composable private fun ShopCheckInRatingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInRating(ShopRating(3, 4, 4))
}
