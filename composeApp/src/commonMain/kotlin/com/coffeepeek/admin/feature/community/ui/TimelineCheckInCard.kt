package com.coffeepeek.admin.feature.community.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.admin.feature.checkin.api.CheckInTimelineContent
import com.coffeepeek.admin.ui.component.blockableAuthor
import com.coffeepeek.admin.ui.component.CheckInActions
import com.coffeepeek.core.designsystem.component.CoffeePeekPhotoViewerImages
import com.coffeepeek.domain.feature.feed.FeedHomePreparation
import com.coffeepeek.domain.model.CheckIn
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility

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
    onPhotoClick: (CoffeePeekPhotoViewerImages, Int) -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (() -> Unit)? = null,
    canBlockAuthor: Boolean = !isOwn,
) {
    CheckInTimelineContent(
        checkIn = checkIn,
        isOwn = isOwn,
        onShopClick = onShopClick.takeIf { checkIn.shopId.isNotBlank() },
        onPhotoClick = onPhotoClick,
        modifier = modifier,
        onAuthorClick = onAuthorClick,
        onHelpful = onHelpful.takeIf { canAct && isPublicTimeline },
        showHelpfulCount = isPublicTimeline || checkIn.helpfulCount > 0,
        actions = {
            CheckInActions(
                onEditClick = onEdit.takeIf { isOwn },
                onReportClick = onReport.takeIf { isPublicTimeline && !isOwn },
                onHideClick = onVisibility.takeIf { isOwn && checkIn.visibility == CheckInVisibility.Public },
                enabled = canAct,
                isHiding = changingVisibility,
                author = checkIn.blockableAuthor().takeIf { isPublicTimeline && !isOwn && canBlockAuthor },
            )
        },
        supportingContent = {
            if (!isPublicTimeline) Text(checkIn.publicationLabel(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isOwn && checkIn.moderationState == CheckInModerationState.Rejected) checkIn.rejectionReason?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        },
        footerActions = if (isOwn && checkIn.visibility == CheckInVisibility.Private) {
            {
                TextButton(onClick = onVisibility, enabled = canAct) {
                    Text(if (changingVisibility) "Сохраняем…" else "Опубликовать")
                }
            }
        } else null,
    )
}

internal fun FeedHomePreparation.timelineDetails(): String = listOfNotNull(
    brewMethodCode?.methodLabel(),
    doseGrams?.let { "$it г" },
    brewDurationSeconds?.let { "$it с" },
    tasteScore?.let { "$it/5" },
).joinToString(" · ")
