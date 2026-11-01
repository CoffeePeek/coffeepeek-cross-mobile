package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_rating_summary
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_rating_sample
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_rating_category
import com.coffeepeek.feature.shop.impl.resources.shop_review_coffee
import com.coffeepeek.feature.shop.impl.resources.shop_review_service
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_aura
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_rating_coffee
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_rating_service
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_rating_atmosphere
import com.coffeepeek.feature.shop.impl.ui.data.formatShopRating
import com.coffeepeek.feature.shop.impl.ui.data.ratingSummary
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInRatingSummary
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInRatingsOverview(checkIns: List<ShopCheckIn>, overallRating: Double?,
    checkInCount: Int, modifier: Modifier = Modifier,
    summary: ShopCheckInRatingSummary? = checkIns.ratingSummary(),
    sampleSize: Int = checkIns.size,
) {
    val averages = summary ?: return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            Icon(CoffeePeekIcons.StarFilled, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(Res.string.shop_checkins_rating_summary,
                formatShopRating(overallRating?.takeIf { it > 0 && it.isFinite() } ?: averages.overall),
                maxOf(checkInCount, sampleSize)),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (checkInCount > sampleSize) Text(stringResource(Res.string.shop_checkins_rating_sample),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RatingRow(stringResource(Res.string.shop_review_coffee), averages.coffee, Res.drawable.shop_checkin_rating_coffee)
        RatingRow(stringResource(Res.string.shop_review_service), averages.service, Res.drawable.shop_checkin_rating_service)
        RatingRow(stringResource(Res.string.shop_checkins_aura), averages.place, Res.drawable.shop_checkin_rating_atmosphere)
    }
}

@Composable
private fun RatingRow(label: String, value: Double, mascot: DrawableResource) {
    val formatted = formatShopRating(value)
    val description = stringResource(Res.string.shop_checkins_rating_category, label, formatted)
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Row(Modifier.width(112.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            Image(painterResource(mascot), contentDescription = null, modifier = Modifier.size(28.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth((value / 5).toFloat().coerceIn(0f, 1f)).height(6.dp)
                .background(MaterialTheme.colorScheme.primary))
        }
        Text(formatted, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview @Composable private fun ShopCheckInRatingsOverviewLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInRatingsOverview(listOf(previewCheckIn()), 4.5, 12)
}
@Preview @Composable private fun ShopCheckInRatingsOverviewDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInRatingsOverview(listOf(previewCheckIn()), null, 1)
}
