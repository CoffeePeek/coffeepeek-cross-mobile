package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_rating_value
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopRatingField(
    label: String,
    value: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        CoffeePeekRating(value.toDouble(), size = CoffeePeekRatingSize.Large,
            onRatingChange = onChange, enabled = enabled,
            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
            starDescription = { stringResource(Res.string.shop_review_rating_value, label, it) })
    }
}

@Preview @Composable private fun ShopRatingFieldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopRatingField("Кофе", 4, true, {})
}

@Preview @Composable private fun ShopRatingFieldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopRatingField("Кофе", 2, true, {})
}
