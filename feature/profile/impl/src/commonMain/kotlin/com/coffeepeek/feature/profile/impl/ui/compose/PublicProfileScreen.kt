package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileStats
import com.coffeepeek.feature.profile.impl.ui.PublicProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.compose.component.PublicProfileConnectionsSheet
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileEvent
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Owner-specific content and destinations supplied by the application composition boundary. */
internal class OwnProfileExtras(
    val onOpenFavorites: () -> Unit,
    val onOpenCheckIns: () -> Unit,
    val onOpenContributions: (ProfileContribution) -> Unit,
    val topBarActions: @Composable () -> Unit,
    val favoritesPreview: @Composable (header: @Composable () -> Unit) -> Unit,
    val checkInCount: Int,
    val activityPreview: @Composable (onClick: () -> Unit) -> Unit,
    val bottomContentPadding: Dp,
)

internal class PublicProfileCallbacks(
    val onBack: (() -> Unit)?,
    val onOpenShop: (String) -> Unit,
    val onOpenUser: (slug: String, nickname: String, avatarUrl: String?) -> Unit,
    val onShare: (String) -> Unit,
    val onOpenCheckIns: () -> Unit,
    val onAction: (PublicProfileAction) -> Unit,
    val onPhotoClick: (previews: List<String>, originals: List<String>, index: Int) -> Unit,
)

private data class ProfilePhotos(val previews: List<String>, val originals: List<String>, val index: Int)

/** Lifecycle adapter; both the week and the check-in section use the caller's full-history destination. */
@Composable
internal fun PublicProfileScreen(
    slug: String, nickname: String?, avatarUrl: String?, vm: PublicProfileViewModel,
    authorActions: ProfileAuthorActions, slots: ProfileSlots, onBack: (() -> Unit)?,
    onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit, onLogin: () -> Unit,
    onOpenUser: (slug: String, nickname: String, avatarUrl: String?) -> Unit,
    onShare: (url: String) -> Unit, onOpenCheckIns: () -> Unit,
    checkInsOnly: Boolean = false, own: OwnProfileExtras? = null,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val visibility by vm.authorVisibility.collectAsStateWithLifecycle(initialValue = vm.currentVisibility())
    var photos by remember(slug) { mutableStateOf<ProfilePhotos?>(null) }
    val pending = !visibility.ready || state.profile?.let { !it.isBlockedByViewer && visibility.hides(it.authorId, it.slug) } == true
    val visible = if (pending) state.copy(profile = null, checkIns = emptyList(), connectionKind = null) else state
    val callbacks = PublicProfileCallbacks(onBack, onOpenShop, onOpenUser, onShare,
        own?.onOpenCheckIns ?: onOpenCheckIns, vm::onAction,
        { previews, originals, index -> photos = ProfilePhotos(previews, originals, index) })
    LaunchedEffect(visibility.revision, visible.profile?.slug) { photos = null }
    LaunchedEffect(vm) { vm.events.collect { if (it == PublicProfileEvent.RequireLogin) onLogin() } }
    if (checkInsOnly) {
        PublicProfileCheckInsScreenContent(visible, slots, onBack ?: {}, onOpenShop,
            { vm.onAction(if (visible.profile == null) PublicProfileAction.Refresh else PublicProfileAction.RetryCheckIns) },
            { vm.onAction(PublicProfileAction.LoadMoreCheckIns) }, callbacks.onPhotoClick)
    } else PublicProfileScreenContent(visible, pending, authorActions, slots, callbacks, own)
    if (!checkInsOnly) PublicProfileConnections(visible, slots, callbacks)
    photos?.takeIf { visible.profile?.isBlockedByViewer == false }?.let {
        slots.photoViewer(it.previews, it.originals, it.index) { photos = null }
    }
}

@Composable
internal fun PublicProfileScreenContent(
    state: PublicProfileState, visibilityPending: Boolean, authorActions: ProfileAuthorActions,
    slots: ProfileSlots, callbacks: PublicProfileCallbacks, own: OwnProfileExtras? = null,
) {
    val viewingOwn = own != null || state.isOwnProfile
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            PublicProfileTopBar(state.profile, viewingOwn, own, authorActions, callbacks)
            PublicProfileBody(state, visibilityPending, viewingOwn, authorActions, slots, callbacks, own)
        }
    }
}

@Composable
private fun PublicProfileConnections(state: PublicProfileState, slots: ProfileSlots, callbacks: PublicProfileCallbacks) {
    if (state.connectionKind == null || state.profile?.isBlockedByViewer != false) return
    PublicProfileConnectionsSheet(
        state = state, image = slots.image,
        onDismiss = { callbacks.onAction(PublicProfileAction.CloseConnections) },
        onSelectKind = { callbacks.onAction(PublicProfileAction.OpenConnections(it)) },
        onLoadMore = { callbacks.onAction(PublicProfileAction.LoadMoreConnections) },
        onRetry = { callbacks.onAction(PublicProfileAction.RetryConnections) },
        onUserClick = {
            callbacks.onAction(PublicProfileAction.CloseConnections)
            callbacks.onOpenUser(it.slug, it.userName, it.avatarUrl)
        },
    )
}

@Composable
private fun PublicProfileScreenPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val profile = PublicProfile("coffeepeek", "coffeepeek", "Кофейные заметки", null,
        PublicProfileStats(0, 2, 1, 4), emptyList(), null)
    val slots = ProfileSlots(image = { _, _, _ -> }, photoViewer = { _, _, _, _ -> }, photoPrefetch = { _, _ -> })
    PublicProfileScreenContent(PublicProfileState(isLoading = false, profile = profile), false,
        ProfileAuthorActions.None, slots, PublicProfileCallbacks(null, {}, { _, _, _ -> }, {}, {}, {}, { _, _, _ -> }))
}

@Preview @Composable private fun PublicProfileLightPreview() = PublicProfileScreenPreview(false)
@Preview @Composable private fun PublicProfileDarkPreview() = PublicProfileScreenPreview(true)
