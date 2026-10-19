package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.domain.model.*
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.data.displayName
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCheckInCard(checkIn: PersonalCheckIn, dateLabel: String,
    onOpenPhoto: (Int) -> Unit, modifier: Modifier = Modifier, imageContent: HistoryImageContent? = null) {
    val shopTitle = checkIn.shopName.ifBlank { stringResource(Res.string.history_shop) }
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            HistoryCheckInHeader(checkIn.username, dateLabel, avatarUrl = checkIn.authorAvatarUrl, imageContent = imageContent)
            Text(shopTitle, Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            checkIn.rating?.let { HistoryCheckInRating(it) }
            HistoryCheckInNote(checkIn.note)
            HistoryCheckInPhotos(checkIn.photoThumbnailUrls ?: checkIn.photoUrls, onOpenPhoto, imageContent = imageContent,
                originalUrls = checkIn.photoUrls)
            if (checkIn.visibility == HistoryVisibility.Public || checkIn.helpfulCount > 0) {
                val likes = stringResource(Res.string.history_helpful_count, checkIn.helpfulCount)
                Row(Modifier.semantics(mergeDescendants = true) { contentDescription = likes },
                    horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                    Icon(if (checkIn.isHelpfulByCurrentUser) CoffeePeekIcons.FavoriteFilled else CoffeePeekIcons.Favorite,
                        null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(checkIn.helpfulCount.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
            checkIn.drink.displayName(Locale.current.language)?.let { HistoryDrinkBadge(it) }
        }
    }
}

internal fun previewHistoryCheckIn() = PersonalCheckIn(
    id = "preview", shop = HistoryAddress("coffee", "/coffee-shop/coffee"), shopName = "Кофейня",
    author = null, username = "Анна", note = "Уютное место и отличный кофе.",
    createdAt = "2026-10-14T10:00:00Z", visitedAt = "2026-10-14T10:00:00Z", rating = HistoryRating(4, 4, 5),
    photoUrls = emptyList(), drink = HistoryDrink("cappuccino", null, "Капучино", "Cappuccino"),
    visibility = HistoryVisibility.Private, moderation = HistoryModeration.NotSubmitted, contentRevision = 0,
    rejectionReason = null, helpfulCount = 0, isHelpfulByCurrentUser = false,
)

@PreviewLightDark @Composable private fun HistoryCheckInUnavailablePhotoPreview() = CoffeePeekTheme {
    HistoryCheckInCard(previewHistoryCheckIn().copy(photoUrls = listOf("fixture-original"),
        photoThumbnailUrls = listOf("")), "14 октября", {})
}

@PreviewLightDark @Composable private fun HistoryCheckInCardPreview() = CoffeePeekTheme {
    HistoryCheckInCard(previewHistoryCheckIn(), "14 октября 2026", {})
}
