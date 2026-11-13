package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_additional_ratings
import com.coffeepeek.feature.shop.impl.resources.shop_review_coffee
import com.coffeepeek.feature.shop.impl.resources.shop_review_place
import com.coffeepeek.feature.shop.impl.resources.shop_review_rating_value
import com.coffeepeek.feature.shop.impl.resources.shop_review_service
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInCoffeeRating(rating: ShopRating, enabled: Boolean, onChange: (ShopRating) -> Unit) {
    ShopCheckInRatingField(stringResource(Res.string.shop_review_coffee), rating.coffee, enabled) {
        onChange(rating.copy(coffee = it))
    }
}

@Composable
internal fun ShopCheckInAdditionalRatings(rating: ShopRating, enabled: Boolean, onChange: (ShopRating) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CoffeePeekTextButton(onClick = { expanded = !expanded }, enabled = enabled) {
            Text(stringResource(Res.string.shop_checkin_additional_ratings))
        }
        if (expanded) {
            ShopCheckInRatingField(stringResource(Res.string.shop_review_service), rating.service, enabled) {
                onChange(rating.copy(service = it))
            }
            ShopCheckInRatingField(stringResource(Res.string.shop_review_place), rating.place, enabled) {
                onChange(rating.copy(place = it))
            }
        }
    }
}

@Composable
private fun ShopCheckInRatingField(label: String, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        CoffeePeekRating(value.toDouble(), size = CoffeePeekRatingSize.Large,
            onRatingChange = onChange, enabled = enabled,
            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            starDescription = { stringResource(Res.string.shop_review_rating_value, label, it) })
    }
}

@Preview @Composable private fun ShopCheckInRatingsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    Column {
        ShopCheckInCoffeeRating(ShopRating(4, 4, 4), true, {})
        ShopCheckInAdditionalRatings(ShopRating(4, 4, 4), true, {})
    }
}

@Preview @Composable private fun ShopCheckInRatingsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    Column {
        ShopCheckInCoffeeRating(ShopRating(4, 4, 4), true, {})
        ShopCheckInAdditionalRatings(ShopRating(4, 4, 4), true, {})
    }
}
