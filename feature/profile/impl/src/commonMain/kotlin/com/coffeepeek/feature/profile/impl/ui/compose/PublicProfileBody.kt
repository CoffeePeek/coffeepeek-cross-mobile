package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.profile.api.ProfileAuthor
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileConnectionKind
import com.coffeepeek.feature.profile.impl.resources.*
import com.coffeepeek.feature.profile.impl.ui.compose.component.InlineProfileError
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileLoadingSkeleton
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileSectionHeading
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileSectionLoader
import com.coffeepeek.feature.profile.impl.ui.compose.component.PublicProfileActivityPreview
import com.coffeepeek.feature.profile.impl.ui.compose.component.PublicProfileCheckInCard
import com.coffeepeek.feature.profile.impl.ui.compose.component.PublicProfileHeader
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileError
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState

@Composable
internal fun PublicProfileBody(state: PublicProfileState, visibilityPending: Boolean, viewingOwn: Boolean,
    authorActions: ProfileAuthorActions, slots: ProfileSlots, callbacks: PublicProfileCallbacks, own: OwnProfileExtras?) {
    val profile = state.profile
    when {
        profile == null -> PublicProfileUnavailable(state, visibilityPending) { callbacks.onAction(PublicProfileAction.Refresh) }
        profile.isBlockedByViewer -> PublicProfileBlocked(profile, authorActions)
        else -> PublicProfileLoadedContent(state, profile, viewingOwn, authorActions, slots, callbacks, own)
    }
}

@Composable
private fun PublicProfileUnavailable(state: PublicProfileState, visibilityPending: Boolean, onRetry: () -> Unit) {
    if (state.isLoading || visibilityPending) {
        ProfileLoadingSkeleton()
        return
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 68.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(CoffeePeekIcons.User, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp))
        Text(stringResource(when (state.error) {
            PublicProfileError.BlocksLoad -> Res.string.profile_blocks_load_failed
            PublicProfileError.Unavailable -> Res.string.profile_unavailable
            else -> Res.string.profile_load_failed
        }), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        CoffeePeekFilledButton(onRetry) { Text(stringResource(Res.string.profile_retry_full)) }
    }
}

@Composable
internal fun PublicProfileBlocked(profile: PublicProfile, authorActions: ProfileAuthorActions) {
    val author = ProfileAuthor(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl)
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(CoffeePeekIcons.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp))
        Text(stringResource(Res.string.profile_blocked_title), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(stringResource(Res.string.profile_blocked_message, profile.nickname), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        CoffeePeekFilledButton({ authorActions.requestUnblock(author) }) { Text(stringResource(Res.string.profile_unblock)) }
    }
}

@Composable
private fun PublicProfileLoadedContent(state: PublicProfileState, profile: PublicProfile, viewingOwn: Boolean,
    authorActions: ProfileAuthorActions, slots: ProfileSlots, callbacks: PublicProfileCallbacks, own: OwnProfileExtras?) {
    ProfileContentTabs(
        profile.slug,
        checkIns = { PublicProfileCheckInPreview(state, profile, viewingOwn, authorActions, slots, callbacks) },
        collections = { PublicProfileCollections(profile, viewingOwn, slots, own) },
        shelf = { slots.shelfPreview(profile.slug, viewingOwn) },
        modifier = Modifier.fillMaxSize().then(if (own == null) Modifier.navigationBarsPadding() else Modifier),
        ownerShelf = own?.shelf, bottomPadding = own?.bottomContentPadding ?: 20.dp,
    ) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(2.dp))
            PublicProfileIdentity(state, profile, viewingOwn, slots, callbacks, own)
            PublicProfileWeek(state, profile, slots, callbacks, own)
            PublicProfileRefreshErrors(state, viewingOwn, callbacks.onAction)
        }
    }
}

@Composable
private fun PublicProfileIdentity(state: PublicProfileState, profile: PublicProfile, viewingOwn: Boolean,
    slots: ProfileSlots, callbacks: PublicProfileCallbacks, own: OwnProfileExtras?) {
    val contributions = own?.onOpenContributions ?: slots.onOwnContributions
    PublicProfileHeader(profile.nickname, profile.avatarUrl, profile.about, slots.image,
        isFollowing = state.isFollowing, followEnabled = !state.isFollowLoading && !state.isLoading,
        onFollow = if (viewingOwn) null else ({ callbacks.onAction(PublicProfileAction.ToggleFollow) }),
        stats = profile.stats,
        onShopsClick = if (viewingOwn) ({ contributions(ProfileContribution.Shops) }) else null,
        onRoastersClick = if (viewingOwn) ({ contributions(ProfileContribution.Roasters) }) else null,
        onFollowersClick = { callbacks.onAction(PublicProfileAction.OpenConnections(PublicProfileConnectionKind.Followers)) },
        onAvatarClick = { profile.avatarUrl?.let { callbacks.onPhotoClick(listOf(it), listOf(it), 0) } })
}

@Composable
private fun PublicProfileWeek(state: PublicProfileState, profile: PublicProfile, slots: ProfileSlots,
    callbacks: PublicProfileCallbacks, own: OwnProfileExtras?) {
    if (own != null) own.activityPreview(callbacks.onOpenCheckIns)
    else PublicProfileActivityPreview(profile.slug, profile.activity, state.checkIns,
        (state.isLoadingActivity && profile.activity == null) || (state.isLoadingCheckIns && state.checkIns.isEmpty()),
        state.activityError != null || state.checkInsError != null, slots.image,
        {
            callbacks.onAction(PublicProfileAction.RetryActivity)
            callbacks.onAction(PublicProfileAction.RetryCheckIns)
        }, callbacks.onOpenCheckIns)
}

@Composable
private fun PublicProfileRefreshErrors(state: PublicProfileState, viewingOwn: Boolean, onAction: (PublicProfileAction) -> Unit) {
    if (state.error != null) InlineProfileError(stringResource(Res.string.profile_update_failed)) { onAction(PublicProfileAction.Refresh) }
    if (!viewingOwn && state.followError != null) InlineProfileError(stringResource(Res.string.profile_follow_failed)) {
        onAction(PublicProfileAction.ToggleFollow)
    }
}

@Composable
private fun PublicProfileCollections(profile: PublicProfile, viewingOwn: Boolean, slots: ProfileSlots, own: OwnProfileExtras?) {
    when {
        own != null -> own.favoritesPreview {
            ProfileSectionHeading(stringResource(Res.string.profile_section_favorites), onAll = own.onOpenFavorites)
        }
        viewingOwn -> Column {
            Box(Modifier.padding(horizontal = 16.dp)) {
                ProfileSectionHeading(stringResource(Res.string.profile_section_favorites), onAll = slots.onOpenOwnCollections)
            }
            slots.ownerCollection()
        }
        else -> slots.featuredCollection(profile.slug)
    }
}

@Composable
private fun PublicProfileCheckInPreview(state: PublicProfileState, profile: PublicProfile, viewingOwn: Boolean,
    authorActions: ProfileAuthorActions, slots: ProfileSlots, callbacks: PublicProfileCallbacks) {
    val author = ProfileAuthor(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileSectionHeading(stringResource(Res.string.profile_section_check_ins), onAll = callbacks.onOpenCheckIns)
        if (state.checkInsError != null && state.checkIns.isNotEmpty()) {
            InlineProfileError(stringResource(Res.string.profile_check_ins_failed)) { callbacks.onAction(PublicProfileAction.RetryCheckIns) }
        }
        when {
            state.isLoadingCheckIns && state.checkIns.isEmpty() -> ProfileSectionLoader(100.dp)
            state.checkInsError != null && state.checkIns.isEmpty() ->
                InlineProfileError(stringResource(Res.string.profile_check_ins_failed)) { callbacks.onAction(PublicProfileAction.RetryCheckIns) }
            state.checkIns.isEmpty() -> Text(stringResource(Res.string.profile_check_ins_empty),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> state.checkIns.take(2).forEach { checkIn ->
                val allowActions = state.isSignedIn && !viewingOwn
                PublicProfileCheckInCard(checkIn, author, slots,
                    checkIn.coffeeShopSlug?.takeIf(String::isNotBlank)?.let { shop -> { callbacks.onOpenShop(shop) } },
                    callbacks.onPhotoClick,
                    onBlock = if (allowActions) ({ authorActions.requestBlock(author) }) else null,
                    onReport = if (allowActions) ({ slots.reportCheckIn(checkIn.id) }) else null)
            }
        }
    }
}
