package com.coffeepeek.admin.feature.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import com.coffeepeek.admin.ui.component.CheckInActions
import com.coffeepeek.admin.ui.component.CheckInCard
import com.coffeepeek.admin.ui.component.CheckInHeader
import com.coffeepeek.admin.ui.component.CheckInHeading
import com.coffeepeek.admin.ui.component.HelpfulButton
import com.coffeepeek.admin.ui.component.ReviewPhotoStrip
import com.coffeepeek.admin.ui.component.SavedDrinkBadge
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
        CheckInHeader(checkIn, isOwn) {
            CheckInActions(
                onEditClick = onEdit.takeIf { isOwn },
                onReportClick = onReport.takeIf { isPublicTimeline && !isOwn },
                onHideClick = onVisibility.takeIf { isOwn && checkIn.visibility == CheckInVisibility.Public },
                enabled = canAct,
                isHiding = changingVisibility,
            )
        }
        CheckInHeading(
            shopName = checkIn.shopName.ifBlank { "Кофейня" },
            rating = checkIn.rating,
            onShopClick = onShopClick.takeIf { checkIn.shopId.isNotBlank() },
        )
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
