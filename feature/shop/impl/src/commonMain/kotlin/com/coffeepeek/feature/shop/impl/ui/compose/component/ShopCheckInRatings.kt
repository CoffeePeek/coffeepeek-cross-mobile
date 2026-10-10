package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_coffee
import com.coffeepeek.feature.shop.impl.resources.shop_review_service
import com.coffeepeek.feature.shop.impl.resources.shop_review_place
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInRatings(rating: ShopRating, enabled: Boolean, onChange: (ShopRating) -> Unit) {
    Column {
        ShopRatingField(stringResource(Res.string.shop_review_coffee), rating.coffee, enabled) {
            onChange(rating.copy(coffee = it))
        }
        ShopRatingField(stringResource(Res.string.shop_review_service), rating.service, enabled) {
            onChange(rating.copy(service = it))
        }
        ShopRatingField(stringResource(Res.string.shop_review_place), rating.place, enabled) {
            onChange(rating.copy(place = it))
        }
    }
}

@Preview @Composable private fun ShopCheckInRatingsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInRatings(ShopRating(4, 4, 4), true, {})
}
@Preview @Composable private fun ShopCheckInRatingsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInRatings(ShopRating(4, 4, 4), true, {})
}
