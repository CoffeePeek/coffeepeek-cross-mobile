package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_overview_reviews
import com.coffeepeek.feature.shop.impl.resources.shop_overview_reviews_one
import com.coffeepeek.feature.shop.impl.resources.shop_overview_reviews_few
import com.coffeepeek.feature.shop.impl.resources.shop_overview_no_rating
import com.coffeepeek.feature.shop.impl.resources.shop_overview_distance
import com.coffeepeek.feature.shop.impl.resources.shop_price_title
import com.coffeepeek.feature.shop.impl.resources.shop_price_average_value
import com.coffeepeek.feature.shop.impl.resources.shop_price_high_value
import com.coffeepeek.feature.shop.impl.resources.shop_price_low_value
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopPriceLevel
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.round

/** Distance comes from app composition's existing location state, without permission requests here. */
@Composable
internal fun ShopOverviewStats(
    overview: ShopOverview,
    onOpenCheckIns: () -> Unit,
    onOpenPriceInfo: () -> Unit,
    modifier: Modifier = Modifier,
    distanceLabel: String? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            ShopOverviewInfoCard(
                title = overview.rating?.let(::formatRating) ?: stringResource(Res.string.shop_overview_no_rating),
                subtitle = stringResource(when {
                    overview.reviewCount % 100 in 11..14 -> Res.string.shop_overview_reviews
                    overview.reviewCount % 10 == 1 -> Res.string.shop_overview_reviews_one
                    overview.reviewCount % 10 in 2..4 -> Res.string.shop_overview_reviews_few
                    else -> Res.string.shop_overview_reviews
                }, overview.reviewCount),
                icon = CoffeePeekIcons.StarFilled,
                modifier = Modifier.weight(1.3f).fillMaxHeight(),
                onClick = onOpenCheckIns, showChevron = true,
            )
            distanceLabel?.takeIf(String::isNotBlank)?.let { label ->
                ShopOverviewInfoCard(label, stringResource(Res.string.shop_overview_distance),
                    CoffeePeekIcons.Distance, Modifier.weight(1f).fillMaxHeight())
            }
            ShopPriceLevel.from(overview.priceRange)?.let { level ->
                ShopOverviewInfoCard(stringResource(level.valueResource()),
                    stringResource(Res.string.shop_price_title), CoffeePeekIcons.Coins,
                    Modifier.weight(1.2f).fillMaxHeight(), onClick = onOpenPriceInfo)
            }
        }
    }
}

private fun formatRating(rating: Double): String {
    val tenths = round(rating * 10.0).toInt()
    return "${tenths / 10}.${tenths % 10}"
}

private fun ShopPriceLevel.valueResource(): StringResource = when (this) {
    ShopPriceLevel.Low -> Res.string.shop_price_low_value
    ShopPriceLevel.Average -> Res.string.shop_price_average_value
    ShopPriceLevel.High -> Res.string.shop_price_high_value
}

@Preview @Composable private fun ShopOverviewStatsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopOverviewStats(previewOverview(), {}, {}, distanceLabel = "350 м")
}

@Preview @Composable private fun ShopOverviewStatsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopOverviewStats(previewOverview().copy(isOpen = false), {}, {}, distanceLabel = "350 м")
}

private fun previewOverview() = ShopOverview(
    id = "preview", title = "Кофейня", description = null,
    address = null, latitude = null, longitude = null,
    rating = 4.6, reviewCount = 12, isOpen = true, photos = emptyList(),
    priceRange = "$$",
)
