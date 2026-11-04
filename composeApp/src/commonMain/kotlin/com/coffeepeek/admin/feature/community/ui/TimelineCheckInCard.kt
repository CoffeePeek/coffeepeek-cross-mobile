package com.coffeepeek.admin.feature.community.ui

import com.coffeepeek.admin.ui.component.PreparationMetricLabels

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.admin.feature.checkin.api.CheckInTimelineContent
import com.coffeepeek.admin.ui.component.blockableAuthor
import com.coffeepeek.admin.ui.component.CheckInActions
import com.coffeepeek.admin.ui.component.PhotoViewerImages
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
    onPhotoClick: (PhotoViewerImages, Int) -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (() -> Unit)? = null,
    canBlockAuthor: Boolean = !isOwn,
) {
    val localizedNotificationsSaving = stringResource(Res.string.notifications_saving)
    val localizedLegacyTimelineCheckInCardCommunityPublish = stringResource(Res.string.legacy_timeline_check_in_card_community_publish)

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
                author = checkIn.blockableAuthor(stringResource(Res.string.legacy_author_fallback)).takeIf { isPublicTimeline && !isOwn && canBlockAuthor },
            )
        },
        supportingContent = {
            if (!isPublicTimeline) Text(checkIn.publicationLabel(checkInPublicationLabels()), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isOwn && checkIn.moderationState == CheckInModerationState.Rejected) checkIn.rejectionReason?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        },
        footerActions = if (isOwn && checkIn.visibility == CheckInVisibility.Private) {
            {
                TextButton(onClick = onVisibility, enabled = canAct) {
                    Text(if (changingVisibility) localizedNotificationsSaving else localizedLegacyTimelineCheckInCardCommunityPublish)
                }
            }
        } else null,
    )
}

internal fun FeedHomePreparation.timelineDetails(labels: PreparationMetricLabels): String = listOfNotNull(
    brewMethodCode?.methodLabel(labels.otherMethod),
    doseGrams?.let { labels.grams.replace("%1\$s", it.toString()) },
    brewDurationSeconds?.let { labels.seconds.replace("%1\$s", it.toString()) },
    tasteScore?.let { "$it/5" },
).joinToString(" · ")
