package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
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
    Row(modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(5) { index ->
                val filled = index < rating.average.toInt().coerceIn(0, 5)
                Icon(if (filled) CoffeePeekIcons.StarFilled else CoffeePeekIcons.StarOutline,
                    contentDescription = null, modifier = Modifier.size(20.dp),
                    tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            }
        }
        Text(value, Modifier.padding(start = CoffeePeekDimens.spacing2), style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview @Composable private fun ShopCheckInRatingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInRating(ShopRating(4, 5, 5))
}
@Preview @Composable private fun ShopCheckInRatingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInRating(ShopRating(3, 4, 4))
}
