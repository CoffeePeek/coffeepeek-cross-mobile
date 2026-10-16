package com.coffeepeek.admin.feature.checkin.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.domain.model.CheckIn

/** Check-in presentation entry point; callers supply only the actions their screen supports. */
@Composable
internal fun CheckInTimelineContent(
    checkIn: CheckIn,
    isOwn: Boolean,
    onShopClick: (() -> Unit)?,
    onPhotoClick: (List<String>, Int) -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (() -> Unit)? = null,
    onHelpful: (() -> Unit)? = null,
    showHelpfulCount: Boolean = true,
    actions: @Composable () -> Unit = {},
    supportingContent: @Composable () -> Unit = {},
    footerActions: (@Composable () -> Unit)? = null,
) = com.coffeepeek.admin.feature.checkin.ui.CheckInTimelineContentImpl(
    checkIn = checkIn,
    isOwn = isOwn,
    onShopClick = onShopClick,
    onPhotoClick = onPhotoClick,
    modifier = modifier,
    onAuthorClick = onAuthorClick,
    onHelpful = onHelpful,
    showHelpfulCount = showHelpfulCount,
    actions = actions,
    supportingContent = supportingContent,
    footerActions = footerActions,
)
