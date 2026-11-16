package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.coffeepeek.feature.profile.api.OwnProfileActivity
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.impl.ui.data.PublicProfileTimeZone
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState
import kotlinx.datetime.TimeZone

@Composable
internal fun PublicProfileCheckInsTab(
    state: PublicProfileState, viewingOwn: Boolean,
    authorActions: ProfileAuthorActions, slots: ProfileSlots, callbacks: PublicProfileCallbacks,
    listState: LazyListState, modifier: Modifier, bottomPadding: Dp, ownerActivity: OwnProfileActivity?,
    header: @Composable () -> Unit,
) {
    val onAction = callbacks.onAction
    PublicProfileCheckInsList(
        state, slots, callbacks.onOpenShop, { onAction(PublicProfileAction.RetryCheckIns) },
        { onAction(PublicProfileAction.LoadMoreCheckIns) }, callbacks.onPhotoClick,
        modifier = modifier, isOwnProfile = viewingOwn, authorActions = authorActions,
        listState = listState, header = header, bottomPadding = bottomPadding,
        dateZone = if (ownerActivity == null) PublicProfileTimeZone else TimeZone.currentSystemDefault(),
    )
}
