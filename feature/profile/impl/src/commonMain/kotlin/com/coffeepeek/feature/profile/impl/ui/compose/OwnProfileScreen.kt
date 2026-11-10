package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekDestructiveConfirmationSheet
import com.coffeepeek.feature.profile.api.OwnProfileSlots
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.impl.resources.*
import com.coffeepeek.feature.profile.impl.ui.OwnProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.PublicProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileState
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import kotlinx.coroutines.flow.drop

internal class OwnProfileCallbacks(
    val editProfile: () -> Unit,
    val openShop: (String) -> Unit,
    val openRoaster: (String) -> Unit,
    val login: () -> Unit,
    val openUser: (String, String, String?) -> Unit,
    val openFavorites: () -> Unit,
    val openCheckIns: () -> Unit,
    val openContributions: (ProfileContribution) -> Unit,
    val openBlockedAuthors: () -> Unit,
)

/** Lifecycle adapter for the owner profile; root navigation remains with its caller. */
@Composable
internal fun OwnProfileScreen(
    vm: OwnProfileViewModel,
    publicModel: @Composable (slug: String) -> PublicProfileViewModel,
    authorActions: ProfileAuthorActions,
    slots: OwnProfileSlots,
    bottomClearance: Dp,
    onEditProfile: () -> Unit,
    onOpenShop: (String) -> Unit,
    onOpenRoaster: (String) -> Unit,
    onLogin: () -> Unit,
    onOpenUser: (slug: String, nickname: String, avatarUrl: String?) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenCheckIns: () -> Unit,
    onOpenContributions: (ProfileContribution) -> Unit,
    onOpenBlockedAuthors: () -> Unit,

) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showLogout by remember { mutableStateOf(false) }
    val callbacks = OwnProfileCallbacks(onEditProfile, onOpenShop, onOpenRoaster, onLogin, onOpenUser,
        onOpenFavorites, onOpenCheckIns, onOpenContributions, onOpenBlockedAuthors)
    val actions: @Composable () -> Unit = {
        OwnProfileActions(callbacks.editProfile, callbacks.openContributions, callbacks.openBlockedAuthors) { showLogout = true }
    }
    val bottomPadding = 20.dp + bottomClearance
    if (showLogout) OwnProfileLogoutConfirmation(
        onConfirm = { showLogout = false; vm.onAction(OwnProfileAction.Logout) }, onDismiss = { showLogout = false })
    val slug = state.publicSlug
    val profile = state.profile
    if (state.isLoggedIn && slug != null && profile != null) {
        key(slug) {
            OwnPublicProfileContent(state, publicModel(slug), authorActions, slots, callbacks, actions, bottomPadding)
        }
    } else {
        OwnProfileFallbackScreen(state, slots, callbacks, actions, bottomPadding) { vm.onAction(OwnProfileAction.Refresh) }
    }
}

@Composable
private fun OwnPublicProfileContent(
    state: OwnProfileState, publicVm: PublicProfileViewModel, authorActions: ProfileAuthorActions,
    slots: OwnProfileSlots, callbacks: OwnProfileCallbacks, actions: @Composable () -> Unit, bottomPadding: Dp,
) {
    val profile = state.profile ?: return
    val slug = state.publicSlug ?: return
    ObserveOwnProfileChanges(state, publicVm)
    PublicProfileScreen(
        slug = slug, nickname = profile.userName, avatarUrl = profile.avatarUrl,
        vm = publicVm, authorActions = authorActions, slots = slots.shared, onBack = null,
        onOpenShop = callbacks.openShop, onOpenRoaster = callbacks.openRoaster, onLogin = callbacks.login,
        onOpenUser = callbacks.openUser, onShare = {}, onOpenCheckIns = callbacks.openCheckIns,
        own = OwnProfileExtras(
            onOpenFavorites = callbacks.openFavorites, onOpenCheckIns = callbacks.openCheckIns,
            onOpenContributions = callbacks.openContributions, topBarActions = actions,
            favoritesPreview = { header -> slots.favoritesPreview(callbacks.openShop, callbacks.openRoaster, header) },
            checkInCount = profile.checkInCount, activityPreview = slots.activityPreview,
            bottomContentPadding = bottomPadding, shelf = slots.shelf,
        ),
    )
}

@Composable
private fun ObserveOwnProfileChanges(state: OwnProfileState, publicVm: PublicProfileViewModel) {
    val profile by rememberUpdatedState(state.profile)
    LaunchedEffect(publicVm) {
        snapshotFlow {
            profile?.let {
                listOf(it.address, it.userName, it.about, it.avatarUrl, it.checkInCount,
                    it.addedShopsCount, it.addedRoastersCount, it.followersCount)
            }
        }
            .drop(1)
            .collect { publicVm.onAction(PublicProfileAction.Refresh) }
    }
}

@Composable
private fun OwnProfileLogoutConfirmation(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CoffeePeekDestructiveConfirmationSheet(
        title = stringResource(Res.string.profile_logout_title),
        message = stringResource(Res.string.profile_logout_message),
        confirmLabel = stringResource(Res.string.profile_menu_logout),
        cancelLabel = stringResource(Res.string.profile_cancel),
        onConfirm = onConfirm, onDismiss = onDismiss,
    )
}
