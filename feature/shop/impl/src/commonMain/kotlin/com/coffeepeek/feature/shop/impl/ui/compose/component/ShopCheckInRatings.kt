package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_coffee
import com.coffeepeek.feature.shop.impl.resources.shop_review_service
import com.coffeepeek.feature.shop.impl.resources.shop_review_place
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_rating_coffee
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_rating_service
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_rating_atmosphere
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_ratings
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_ratings_hint
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInRatings(rating: ShopRating, enabled: Boolean, onChange: (ShopRating) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.shop_checkin_form_ratings), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(Res.string.shop_checkin_form_ratings_hint), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ShopCheckInRatingCard(Res.drawable.shop_checkin_rating_coffee, stringResource(Res.string.shop_review_coffee), rating.coffee, enabled) {
            onChange(rating.copy(coffee = it))
        }
        ShopCheckInRatingCard(Res.drawable.shop_checkin_rating_service, stringResource(Res.string.shop_review_service), rating.service, enabled) {
            onChange(rating.copy(service = it))
        }
        ShopCheckInRatingCard(Res.drawable.shop_checkin_rating_atmosphere, stringResource(Res.string.shop_review_place), rating.place, enabled) {
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
