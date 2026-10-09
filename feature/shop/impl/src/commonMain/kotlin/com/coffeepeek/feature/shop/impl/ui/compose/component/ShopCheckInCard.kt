package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_photo_description
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_visited
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_rating
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_author_unknown
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_helpful
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_report
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
internal fun ShopCheckInCard(
    checkIn: ShopCheckIn,
    onOpenPhoto: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
    showAuthor: Boolean = false,
    onVote: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
    showHelpful: Boolean = false,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            if (showAuthor) Text(
                checkIn.username.ifBlank { stringResource(Res.string.shop_checkins_author_unknown) },
                style = MaterialTheme.typography.titleMedium,
            )
            val visited = checkIn.visitedAt.take(10).ifBlank { checkIn.createdAt.take(10) }
            if (visited.isNotBlank()) {
                Text(stringResource(Res.string.shop_checkins_visited, visited),
                    style = MaterialTheme.typography.labelMedium)
            }
            if (checkIn.note.isNotBlank()) Text(checkIn.note, style = MaterialTheme.typography.bodyMedium)
            val drink = checkIn.customDrinkName?.takeIf(String::isNotBlank)
                ?: checkIn.drinkNameRu?.takeIf(String::isNotBlank)
                ?: checkIn.drinkNameEn?.takeIf(String::isNotBlank)
            drink?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            checkIn.rating?.let { rating ->
                val tenths = (rating.average * 10).roundToInt()
                Text(stringResource(Res.string.shop_reviews_rating, "${tenths / 10}.${tenths % 10}"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            if (showHelpful || onReport != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                    if (showHelpful) {
                        val helpful = stringResource(Res.string.shop_checkins_helpful, checkIn.helpfulCount)
                        if (onVote != null) TextButton(onClick = onVote) { Text(helpful) }
                        else Text(helpful, style = MaterialTheme.typography.labelMedium)
                    }
                    onReport?.let { TextButton(onClick = it) { Text(stringResource(Res.string.shop_checkins_report)) } }
                }
            }
            if (checkIn.photoUrls.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                    checkIn.photoUrls.forEachIndexed { index, _ ->
                        ShopPhotoTile(
                            previewUrl = checkIn.photoThumbnailUrls.getOrNull(index) ?: checkIn.photoUrls[index],
                            description = stringResource(Res.string.shop_checkins_photo_description,
                                index + 1, checkIn.photoUrls.size),
                            onClick = { onOpenPhoto(checkIn.photoUrls, index) },
                        )
                    }
                }
            }
        }
    }
}

@Preview @Composable private fun ShopCheckInCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInCard(previewCheckIn(), onOpenPhoto = { _, _ -> }, showAuthor = true,
        showHelpful = true, onVote = {}, onReport = {})
}

@Preview @Composable private fun ShopCheckInCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInCard(previewCheckIn(), onOpenPhoto = { _, _ -> })
}

internal fun previewCheckIn() = ShopCheckIn(
    id = "check-in-1", userId = "user-1", shopId = "shop-1",
    note = "Вкусный кофе и приятная атмосфера.", createdAt = "2026-10-01T12:00:00Z",
    visitedAt = "2026-10-01T11:00:00Z", reviewId = null,
    photoUrls = emptyList(), photoThumbnailUrls = emptyList(), rating = ShopRating(5, 5, 4),
    username = "Алексей", drinkNameRu = "Фильтр", helpfulCount = 3,
)
