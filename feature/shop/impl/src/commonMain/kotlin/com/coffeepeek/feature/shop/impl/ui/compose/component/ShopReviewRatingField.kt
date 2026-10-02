package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_rating_value
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReviewRatingField(
    label: String,
    value: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row {
            (1..5).forEach { star ->
                IconButton(onClick = { onChange(star) }, enabled = enabled) {
                    Icon(
                        imageVector = if (star <= value) CpIcons.StarFilled else CpIcons.StarOutline,
                        contentDescription = stringResource(Res.string.shop_review_rating_value, label, star),
                        tint = if (star <= value) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview @Composable private fun ShopReviewRatingFieldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReviewRatingField("Кофе", 4, true, {})
}

@Preview @Composable private fun ShopReviewRatingFieldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReviewRatingField("Кофе", 2, true, {})
}
