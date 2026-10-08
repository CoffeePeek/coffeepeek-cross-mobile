package com.coffeepeek.admin.feature.community.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CheckInActions
import com.coffeepeek.admin.ui.component.HelpfulButton
import com.coffeepeek.admin.ui.component.formatReviewDisplayDate
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.formatOneDecimal
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.ReviewRating
import com.coffeepeek.domain.model.savedDrinkName

@Composable
internal fun TimelineCheckInCard(
    checkIn: CheckIn,
    isOwn: Boolean,
    isPublicTimeline: Boolean,
    publishedAt: String?,
    canAct: Boolean,
    changingVisibility: Boolean,
    onShopClick: () -> Unit,
    onEdit: () -> Unit,
    onVisibility: () -> Unit,
    onHelpful: () -> Unit,
    onReport: () -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
    photoContent: @Composable (String, Modifier) -> Unit = { url, imageModifier ->
        CpImage(data = url, contentDescription = "Фото визита", contentScale = ContentScale.Crop, modifier = imageModifier)
    },
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.radius2xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                val username = checkIn.username.ifBlank { if (isOwn) "Вы" else "Пользователь" }
                Box(Modifier.size(44.dp).clip(CircleShape).background(CpColor.GoldWarmSoft), contentAlignment = Alignment.Center) {
                    val initials = username.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                    Text(initials, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CpColor.LightTextPrimary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                    Text(username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (isPublicTimeline && !publishedAt.isNullOrBlank()) "Опубликован ${formatReviewDisplayDate(publishedAt)}" else checkIn.publicationLabel(),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                CheckInActions(
                    onEditClick = onEdit.takeIf { isOwn },
                    onReportClick = onReport.takeIf { isPublicTimeline && !isOwn },
                    onHideClick = onVisibility.takeIf { isOwn && checkIn.visibility == CheckInVisibility.Public },
                    enabled = canAct,
                    isHiding = changingVisibility,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                Text(
                    checkIn.shopName.ifBlank { "Кофейня" },
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.then(if (checkIn.shopId.isNotBlank()) Modifier.clickable(onClick = onShopClick) else Modifier),
                )
                checkIn.rating?.let { FeedCheckInRating(it) }
            }
            savedDrinkName(checkIn.drinkNameRu, checkIn.drinkNameEn, checkIn.customDrinkName, Locale.current.language)?.let { drink ->
                Row(
                    Modifier.clip(RoundedCornerShape(percent = 50))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(percent = 50))
                        .padding(horizontal = CpDimens.spacing3, vertical = CpDimens.spacing2),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
                ) {
                    Icon(CpIcons.Coffee, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(drink, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (checkIn.note.isNotBlank()) Text(checkIn.note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (checkIn.photoUrls.isNotEmpty()) BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = minOf(3, checkIn.photoUrls.size)
                val photoWidth = (maxWidth - CpDimens.spacing2 * (columns - 1)) / columns
                LazyRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                    itemsIndexed(checkIn.photoUrls) { index, url ->
                        photoContent(url, Modifier.width(photoWidth).aspectRatio(1.4f).clip(RoundedCornerShape(CpDimens.radiusMd))
                            .clickable { onPhotoClick(checkIn.photoUrls, index) }
                            .semantics { contentDescription = "Фотография ${index + 1} из ${checkIn.photoUrls.size}" })
                    }
                }
            }
            if (isOwn && checkIn.moderationState == CheckInModerationState.Rejected) checkIn.rejectionReason?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if ((isOwn && checkIn.visibility == CheckInVisibility.Private) || (isPublicTimeline && !isOwn) || checkIn.helpfulCount > 0) Row(
                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (isPublicTimeline && !isOwn) HelpfulButton(
                    helpfulCount = checkIn.helpfulCount, isHelpful = checkIn.isHelpfulByCurrentUser,
                    onClick = onHelpful.takeIf { canAct }, showLabel = false,
                ) else if (checkIn.helpfulCount > 0) HelpfulButton(
                    helpfulCount = checkIn.helpfulCount, isHelpful = false, onClick = null, showLabel = false,
                )
                else Spacer(Modifier.weight(1f))
                if (isOwn && checkIn.visibility == CheckInVisibility.Private) TextButton(onClick = onVisibility, enabled = canAct) {
                    Text(if (changingVisibility) "Сохраняем…" else "Опубликовать")
                }
            }
        }
    }
}

@Composable
private fun FeedCheckInRating(rating: ReviewRating) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing1),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Кофе: ${rating.coffee}, сервис: ${rating.service}, место: ${rating.place}. Средняя оценка ${formatOneDecimal(rating.average)} из 5"
        }) {
        (1..5).forEach { star ->
            Icon(CpIcons.StarFilled, null, Modifier.size(18.dp), tint = if (star <= rating.average.toInt())
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
        Text(formatOneDecimal(rating.average), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}
