package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_overview_closed
import com.coffeepeek.feature.shop.impl.resources.shop_overview_open
import com.coffeepeek.feature.shop.impl.resources.shop_overview_reviews
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.round

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShopOverviewStats(overview: ShopOverview, modifier: Modifier = Modifier) {
    FlowRow(modifier.padding(CpDimens.spacing4),
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        overview.rating?.let { rating ->
            Surface(color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium) {
                Text(formatRating(rating), Modifier.padding(CpDimens.spacing2),
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium) {
            Text(stringResource(Res.string.shop_overview_reviews, overview.reviewCount),
                Modifier.padding(CpDimens.spacing2))
        }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium) {
            Text(stringResource(if (overview.isOpen) Res.string.shop_overview_open
                else Res.string.shop_overview_closed), Modifier.padding(CpDimens.spacing2))
        }
    }
}

private fun formatRating(rating: Double): String {
    val tenths = round(rating * 10.0).toInt()
    return "${tenths / 10}.${tenths % 10}"
}

@Preview @Composable private fun ShopOverviewStatsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopOverviewStats(previewOverview())
}

@Preview @Composable private fun ShopOverviewStatsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopOverviewStats(previewOverview().copy(isOpen = false))
}

private fun previewOverview() = ShopOverview(
    id = "preview", title = "Кофейня", description = null,
    address = null, latitude = null, longitude = null,
    rating = 4.6, reviewCount = 12, isOpen = true, photos = emptyList(),
)
