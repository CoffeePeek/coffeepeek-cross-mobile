package com.coffeepeek.admin.feature.community.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CheckInActions
import com.coffeepeek.admin.ui.component.CheckInCard
import com.coffeepeek.admin.ui.component.CheckInRating
import com.coffeepeek.admin.ui.component.HelpfulButton
import com.coffeepeek.admin.ui.component.ReviewAvatar
import com.coffeepeek.admin.ui.component.ReviewPhotoStrip
import com.coffeepeek.admin.ui.component.SavedDrinkBadge
import com.coffeepeek.admin.ui.component.rememberReviewDisplayDate
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility
import com.coffeepeek.domain.model.savedDrinkName

@Composable
internal fun TimelineCheckInCard(
    checkIn: CheckIn,
    isOwn: Boolean,
    isPublicTimeline: Boolean,
    canAct: Boolean,
    changingVisibility: Boolean,
    onShopClick: () -> Unit,
    onEdit: () -> Unit,
    onVisibility: () -> Unit,
    onHelpful: () -> Unit,
    onReport: () -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    CheckInCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
            val username = checkIn.username.ifBlank { if (isOwn) "Вы" else "Пользователь" }
            ReviewAvatar(username, photoUrl = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                Text(username, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    rememberReviewDisplayDate(checkIn.createdAt),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
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
        Text(
            checkIn.shopName.ifBlank { "Кофейня" },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.then(if (checkIn.shopId.isNotBlank()) Modifier.clickable(role = Role.Button, onClick = onShopClick) else Modifier),
        )
        checkIn.rating?.let { CheckInRating(it) }
        savedDrinkName(checkIn.drinkNameRu, checkIn.drinkNameEn, checkIn.customDrinkName, Locale.current.language)?.let { drink ->
            SavedDrinkBadge(drink, checkIn.drinkSlug)
        }
        if (checkIn.note.isNotBlank()) Text(checkIn.note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        ReviewPhotoStrip(checkIn.photoUrls, thumbnailUrls = checkIn.photoThumbnailUrls, onPhotoClick = onPhotoClick)
        if (!isPublicTimeline) Text(checkIn.publicationLabel(), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (isOwn && checkIn.moderationState == CheckInModerationState.Rejected) checkIn.rejectionReason?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (isPublicTimeline || checkIn.helpfulCount > 0 || (isOwn && checkIn.visibility == CheckInVisibility.Private)) Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (isPublicTimeline || checkIn.helpfulCount > 0) HelpfulButton(
                helpfulCount = checkIn.helpfulCount, isHelpful = checkIn.isHelpfulByCurrentUser,
                onClick = onHelpful.takeIf { canAct && isPublicTimeline && !isOwn },
            ) else Spacer(Modifier.weight(1f))
            if (isOwn && checkIn.visibility == CheckInVisibility.Private) TextButton(onClick = onVisibility, enabled = canAct) {
                Text(if (changingVisibility) "Сохраняем…" else "Опубликовать")
            }
        }
    }
}
