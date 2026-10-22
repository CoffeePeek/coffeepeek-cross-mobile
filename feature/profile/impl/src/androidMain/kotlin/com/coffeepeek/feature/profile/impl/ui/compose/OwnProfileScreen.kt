package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekDestructiveConfirmationSheet
import com.coffeepeek.feature.profile.api.OwnProfileSlots
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.domain.PublicProfileStats
import com.coffeepeek.feature.profile.impl.R
import com.coffeepeek.feature.profile.impl.ui.OwnProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.PublicProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileSectionLoader
import com.coffeepeek.feature.profile.impl.ui.compose.component.PublicProfileHeader
import com.coffeepeek.feature.profile.impl.ui.compose.component.PublicProfileStatsCard
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import kotlinx.coroutines.flow.drop

/** The Profile tab: the owner's public profile with owner actions, or a plain summary when the account has no public address. */
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
    val actions: @Composable () -> Unit = {
        OwnProfileActions(onOpenContributions, onOpenBlockedAuthors) { showLogout = true }
    }
    val bottomPadding = 20.dp + bottomClearance

    if (showLogout) CoffeePeekDestructiveConfirmationSheet(
        title = stringResource(R.string.profile_logout_title),
        message = stringResource(R.string.profile_logout_message),
        confirmLabel = stringResource(R.string.profile_menu_logout),
        cancelLabel = stringResource(R.string.profile_cancel),
        onConfirm = { showLogout = false; vm.onAction(OwnProfileAction.Logout) },
        onDismiss = { showLogout = false },
    )

    val slug = state.publicSlug
    val profile = state.profile
    if (state.isLoggedIn && slug != null && profile != null) {
        key(slug) {
            val publicVm = publicModel(slug)
            LaunchedEffect(publicVm) {
                // The tab owns the cached account: a name, bio, avatar or address change reloads the public page.
                snapshotFlow { state.profile?.let { it.address to Triple(it.userName, it.about, it.avatarUrl) } }
                    .drop(1)
                    .collect { publicVm.onAction(PublicProfileAction.Refresh) }
            }
            PublicProfileScreen(
                slug = slug, nickname = profile.userName, avatarUrl = profile.avatarUrl,
                vm = publicVm, authorActions = authorActions, slots = slots.shared,
                onBack = null, onOpenShop = onOpenShop, onOpenRoaster = onOpenRoaster, onLogin = onLogin, onOpenUser = onOpenUser,
                onShare = {},
                own = OwnProfileExtras(
                    onEditProfile = onEditProfile, onOpenFavorites = onOpenFavorites, onOpenCheckIns = onOpenCheckIns,
                    topBarActions = actions,
                    favoritesPreview = { slots.favoritesPreview(onOpenShop, onOpenRoaster) },
                    activityCalendar = slots.activityCalendar,
                    bottomContentPadding = bottomPadding,
                ),
            )
        }
        return
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).statusBarsPadding()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp), contentAlignment = Alignment.CenterStart) {
                Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (state.isLoggedIn) Box(Modifier.align(Alignment.CenterEnd)) { actions() }
            }
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ProfileSectionLoader(160.dp) }
                state.error != null && !state.hasContent -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    slots.loadError { vm.onAction(OwnProfileAction.Refresh) }
                }
                profile != null -> Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PublicProfileHeader(
                        nickname = profile.userName, avatarUrl = profile.avatarUrl, about = profile.about, image = slots.shared.image,
                        onEditProfile = onEditProfile, onAvatarClick = onEditProfile,
                    )
                    PublicProfileStatsCard(PublicProfileStats(profile.checkInCount, profile.addedShopsCount, profile.addedRoastersCount, profile.followersCount))
                    if (state.error != null || state.refreshError != null) slots.loadError { vm.onAction(OwnProfileAction.Refresh) }
                    TextButton(onClick = onOpenFavorites) { Text(stringResource(R.string.profile_own_favorites)) }
                    slots.favoritesPreview(onOpenShop, onOpenRoaster)
                    TextButton(onClick = onOpenCheckIns) { Text(stringResource(R.string.profile_own_check_ins)) }
                    Spacer(Modifier.height(bottomPadding))
                }
            }
        }
    }
}
