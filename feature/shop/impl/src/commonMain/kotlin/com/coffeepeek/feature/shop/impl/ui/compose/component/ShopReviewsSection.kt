package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopReview
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_empty
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_register
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_sign_in
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_sign_in_prompt
import com.coffeepeek.feature.shop.impl.resources.shop_reviews_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReviewsSection(
    reviews: List<ShopReview>,
    shopTitle: String,
    isLoggedIn: Boolean,
    currentUserId: String?,
    onOpenPhoto: (List<String>, Int) -> Unit,
    onVote: (String) -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
        Text(stringResource(Res.string.shop_reviews_title), style = MaterialTheme.typography.titleMedium)
        if (reviews.isEmpty()) {
            Text(stringResource(Res.string.shop_reviews_empty, shopTitle),
                style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                itemsIndexed(reviews, key = { _, review -> review.id }) { index, review ->
                    val hidden = !isLoggedIn && index > 0
                    ShopReviewCard(
                        review = review,
                        canVote = isLoggedIn && currentUserId != review.userId,
                        onVote = { if (!hidden) onVote(review.id) },
                        onOpenPhoto = { urls, photoIndex -> if (!hidden) onOpenPhoto(urls, photoIndex) },
                        modifier = Modifier.width(300.dp).then(
                            if (hidden) Modifier.blur(6.dp).clearAndSetSemantics {} else Modifier,
                        ),
                    )
                }
            }
            if (!isLoggedIn) {
                Card {
                    Column(Modifier.padding(CpDimens.spacing4)) {
                        Text(stringResource(Res.string.shop_reviews_sign_in_prompt),
                            style = MaterialTheme.typography.bodyMedium)
                        Row {
                            TextButton(onClick = onSignIn) {
                                Text(stringResource(Res.string.shop_reviews_sign_in))
                            }
                            TextButton(onClick = onRegister) {
                                Text(stringResource(Res.string.shop_reviews_register))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview @Composable private fun ShopReviewsSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReviewsSection(listOf(previewReview(), previewReview().copy(id = "review-2", username = "Мария")),
        shopTitle = "Кофейня", isLoggedIn = false, currentUserId = null,
        onOpenPhoto = { _, _ -> }, onVote = {}, onSignIn = {}, onRegister = {})
}

@Preview @Composable private fun ShopReviewsSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReviewsSection(listOf(previewReview()), shopTitle = "Кофейня", isLoggedIn = true,
        currentUserId = "user-2", onOpenPhoto = { _, _ -> }, onVote = {},
        onSignIn = {}, onRegister = {})
}
