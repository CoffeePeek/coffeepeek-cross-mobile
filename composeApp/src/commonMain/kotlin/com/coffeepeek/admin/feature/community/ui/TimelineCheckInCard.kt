package com.coffeepeek.admin.feature.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CheckInActions
import com.coffeepeek.admin.ui.component.CheckInHeader
import com.coffeepeek.admin.ui.component.CheckInHeading
import com.coffeepeek.admin.ui.component.HelpfulButton
import com.coffeepeek.admin.ui.component.ReviewPhotoStrip
import com.coffeepeek.admin.ui.component.SavedDrinkLabel
import com.coffeepeek.domain.feature.feed.FeedHomePreparation
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
    onAuthorClick: (() -> Unit)? = null,
) {
    val drinkName = savedDrinkName(checkIn.drinkNameRu, checkIn.drinkNameEn, checkIn.customDrinkName, Locale.current.language)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
    ) {
        CheckInHeader(checkIn, isOwn, onAuthorClick = onAuthorClick) {
            CheckInActions(
                onEditClick = onEdit.takeIf { isOwn },
                onReportClick = onReport.takeIf { isPublicTimeline && !isOwn },
                onHideClick = onVisibility.takeIf { isOwn && checkIn.visibility == CheckInVisibility.Public },
                enabled = canAct,
                isHiding = changingVisibility,
            )
        }
        if (checkIn.isHome) {
            checkIn.homePreparation?.let { recipe ->
                recipe.beanName?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                val details = recipe.timelineDetails()
                if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else CheckInHeading(
            shopName = checkIn.shopName.ifBlank { "Кофейня" },
            rating = checkIn.rating,
            onShopClick = onShopClick.takeIf { checkIn.shopId.isNotBlank() },
        )
        if (checkIn.note.isNotBlank()) Text(checkIn.note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        ReviewPhotoStrip(
            photoUrls = checkIn.photoUrls,
            thumbnailUrls = checkIn.photoThumbnailUrls,
            singlePhotoAspectRatio = 2f,
            onPhotoClick = onPhotoClick,
        )
        if (!isPublicTimeline) Text(checkIn.publicationLabel(), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (isOwn && checkIn.moderationState == CheckInModerationState.Rejected) checkIn.rejectionReason?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (isPublicTimeline || checkIn.helpfulCount > 0 || drinkName != null || (isOwn && checkIn.visibility == CheckInVisibility.Private)) Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
        ) {
            if (isPublicTimeline || checkIn.helpfulCount > 0) HelpfulButton(
                helpfulCount = checkIn.helpfulCount, isHelpful = checkIn.isHelpfulByCurrentUser,
                onClick = onHelpful.takeIf { canAct && isPublicTimeline && !isOwn },
            )
            Spacer(Modifier.weight(1f))
            if (isOwn && checkIn.visibility == CheckInVisibility.Private) TextButton(onClick = onVisibility, enabled = canAct) {
                Text(if (changingVisibility) "Сохраняем…" else "Опубликовать")
            }
            drinkName?.let { SavedDrinkLabel(it) }
        }
    }
}

internal fun FeedHomePreparation.timelineDetails(): String = listOfNotNull(
    brewMethodCode?.methodLabel(),
    doseGrams?.let { "$it г" },
    brewDurationSeconds?.let { "$it с" },
    tasteScore?.let { "$it/5" },
).joinToString(" · ")
