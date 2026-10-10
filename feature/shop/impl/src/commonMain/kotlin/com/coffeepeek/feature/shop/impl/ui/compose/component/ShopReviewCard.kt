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
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReview
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_helpful
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_photo_description
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_rating
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
internal fun ShopReviewCard(
    review: ShopReview,
    canVote: Boolean,
    isVoting: Boolean = false,
    onVote: () -> Unit,
    onOpenPhoto: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            Text(review.username, style = MaterialTheme.typography.titleMedium)
            if (review.header.isNotBlank()) Text(review.header, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(Res.string.shop_reviews_rating, formatReviewRating(review.rating.average)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            if (review.comment.isNotBlank()) Text(review.comment, style = MaterialTheme.typography.bodyMedium)
            if (review.createdAtUtc.isNotBlank()) {
                Text(review.createdAtUtc.take(10), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (review.photoUrls.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                    review.photoUrls.forEachIndexed { index, url ->
                        val description = stringResource(Res.string.shop_reviews_photo_description,
                            index + 1, review.photoUrls.size)
                        ShopPhotoTile(url, description, onClick = { onOpenPhoto(review.photoUrls, index) })
                    }
                }
            }
            if (canVote) {
                TextButton(onClick = onVote, enabled = !isVoting) {
                    Text(stringResource(Res.string.shop_reviews_helpful, review.helpfulCount))
                }
            } else {
                Text(stringResource(Res.string.shop_reviews_helpful, review.helpfulCount),
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun formatReviewRating(rating: Double): String {
    val tenths = (rating * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

@Preview @Composable private fun ShopReviewCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReviewCard(previewReview(), canVote = true, onVote = {}, onOpenPhoto = { _, _ -> })
}

@Preview @Composable private fun ShopReviewCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReviewCard(previewReview(), canVote = false, onVote = {}, onOpenPhoto = { _, _ -> })
}

internal fun previewReview() = ShopReview(
    id = "review-1", moderationReviewId = null, userId = "user-1", shopId = "shop-1",
    username = "Алексей", header = "Отличный кофе", comment = "Уютное место и вкусный фильтр.",
    rating = ShopRating(5, 4, 5), createdAtUtc = "2026-10-01T12:00:00Z",
    photoUrls = emptyList(), helpfulCount = 3, isHelpfulByCurrentUser = false,
)
