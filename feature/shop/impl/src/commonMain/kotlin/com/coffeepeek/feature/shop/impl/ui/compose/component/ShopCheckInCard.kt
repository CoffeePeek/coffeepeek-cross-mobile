package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopCheckIn
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_author_unknown
import com.coffeepeek.feature.shop.impl.resources.shop_checkins_author_self
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInCard(
    checkIn: ShopCheckIn,
    onOpenPhoto: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
    showAuthor: Boolean = false,
    onVote: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
    showHelpful: Boolean = false,
    isOwn: Boolean = false,
    isVoting: Boolean = false,
    shopTitle: String? = null,
    dateLabel: String = checkIn.createdAt.take(10),
    fullVersion: Boolean = false,
    onOpenAuthor: (() -> Unit)? = null,
    onBlockAuthor: (() -> Unit)? = null,
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            if (showAuthor) {
                val author = checkIn.username.ifBlank { stringResource(if (isOwn)
                    Res.string.shop_checkins_author_self else Res.string.shop_checkins_author_unknown) }
                ShopCheckInHeader(author, dateLabel, onReport.takeUnless { isOwn },
                    avatarUrl = checkIn.authorAvatarUrl, onOpenAuthor = onOpenAuthor,
                    onBlockAuthor = onBlockAuthor.takeUnless { isOwn })
            }
            shopTitle?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2)
            }
            checkIn.rating?.let { ShopCheckInRating(it) }
            val primaryDrinkName = if (Locale.current.language == "en") checkIn.drinkNameEn else checkIn.drinkNameRu
            val fallbackDrinkName = if (Locale.current.language == "en") checkIn.drinkNameRu else checkIn.drinkNameEn
            val drink = checkIn.customDrinkName?.takeIf(String::isNotBlank)
                ?: primaryDrinkName?.takeIf(String::isNotBlank) ?: fallbackDrinkName?.takeIf(String::isNotBlank)
            if (checkIn.note.isNotBlank()) ShopCheckInNote(checkIn.id, checkIn.note, fullVersion = fullVersion)
            ShopCheckInPhotos(checkIn.photoUrls, checkIn.photoThumbnailUrls, onOpenPhoto, detailUrls = checkIn.photoDetailUrls)
            val showHelpfulCount = showHelpful &&
                (checkIn.visibility == ShopCheckInVisibility.Public || checkIn.helpfulCount > 0)
            if (showHelpfulCount || drink != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
                    if (showHelpfulCount) ShopCheckInHelpfulButton(checkIn.helpfulCount, checkIn.isHelpfulByCurrentUser,
                        onClick = onVote, isLoading = isVoting)
                    Spacer(Modifier.weight(1f))
                    drink?.let { ShopCheckInDrinkBadge(it) }
                }
            }
        }
    }
}

@Preview @Composable private fun ShopCheckInCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInCard(previewCheckIn(), onOpenPhoto = { _, _ -> }, showAuthor = true,
        showHelpful = true, onVote = {}, onReport = {}, onBlockAuthor = {})
}

@Preview @Composable private fun ShopCheckInCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInCard(previewCheckIn(), onOpenPhoto = { _, _ -> }, showAuthor = true, showHelpful = true,
        isOwn = true, onVote = {}, shopTitle = "Кофейня", dateLabel = "1 окт. 2026")
}

@Preview @Composable private fun ShopCheckInPhotoCardLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInCard(previewCheckIn().copy(photoUrls = List(4) { "" }), { _, _ -> }, showAuthor = true,
        showHelpful = true, shopTitle = "Кофейня", dateLabel = "1 окт. 2026")
}

@Preview @Composable private fun ShopCheckInPhotoCardDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInCard(previewCheckIn().copy(photoUrls = List(4) { "" }), { _, _ -> }, showAuthor = true,
        showHelpful = true, shopTitle = "Кофейня", dateLabel = "1 окт. 2026")
}

internal fun previewCheckIn() = ShopCheckIn(
    id = "check-in-1", userId = "user-1", shopId = "shop-1",
    note = "Вкусный кофе и приятная атмосфера.", createdAt = "2026-10-01T12:00:00Z",
    visitedAt = "2026-10-01T11:00:00Z", reviewId = null,
    photoUrls = emptyList(), photoThumbnailUrls = emptyList(), rating = ShopRating(5, 5, 4),
    username = "Алексей", drinkNameRu = "Фильтр", helpfulCount = 3,
)
