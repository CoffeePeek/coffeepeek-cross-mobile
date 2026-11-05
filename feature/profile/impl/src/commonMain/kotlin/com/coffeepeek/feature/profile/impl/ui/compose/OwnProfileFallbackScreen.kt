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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.OwnProfileSlots
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.domain.PublicProfileStats
import com.coffeepeek.feature.profile.impl.resources.*
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileLoadingSkeleton
import com.coffeepeek.feature.profile.impl.ui.compose.component.ProfileSectionHeading
import com.coffeepeek.feature.profile.impl.ui.compose.component.PublicProfileHeader
import com.coffeepeek.feature.profile.impl.ui.model.OwnProfileState
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Owner presentation when a public profile address has not been configured. */
@Composable
internal fun OwnProfileFallbackScreen(
    state: OwnProfileState, slots: OwnProfileSlots, callbacks: OwnProfileCallbacks,
    actions: @Composable () -> Unit, bottomPadding: Dp, onRetry: () -> Unit,
) {
    var photo by remember(state.profile?.email, state.profile?.avatarUrl) { mutableStateOf<String?>(null) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            OwnProfileFallbackTopBar(state.isLoggedIn, actions)
            when {
                state.isLoading && state.profile == null -> ProfileLoadingSkeleton()
                state.error != null && !state.hasContent -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    slots.loadError(onRetry)
                }
                state.profile != null -> OwnProfileFallbackContent(state, slots, callbacks, bottomPadding, onRetry) { photo = it }
            }
        }
    }
    photo?.let { url -> slots.shared.photoViewer(listOf(url), listOf(url), 0) { photo = null } }
}

@Composable
private fun OwnProfileFallbackTopBar(signedIn: Boolean, actions: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp), contentAlignment = Alignment.CenterStart) {
        Text(stringResource(Res.string.profile_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (signedIn) Box(Modifier.align(Alignment.CenterEnd)) { actions() }
    }
}

@Composable
private fun OwnProfileFallbackContent(
    state: OwnProfileState, slots: OwnProfileSlots, callbacks: OwnProfileCallbacks,
    bottomPadding: Dp, onRetry: () -> Unit, onPhoto: (String) -> Unit,
) {
    val profile = state.profile ?: return
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OwnProfileFallbackHeader(profile, slots, callbacks, onPhoto)
            slots.activityPreview(callbacks.openCheckIns)
            if (state.error != null || state.refreshError != null) slots.loadError(onRetry)
        }
        ProfileContentTabs(
            stateKey = "own:${profile.email}",
            checkIns = { OwnProfileFallbackCheckIns(profile.checkInCount, callbacks.openCheckIns) },
            collections = {
                slots.favoritesPreview(callbacks.openShop, callbacks.openRoaster) {
                    ProfileSectionHeading(stringResource(Res.string.profile_section_favorites), callbacks.openFavorites)
                }
            },
            shelf = { OwnProfileFallbackShelf(callbacks.editProfile) },
        )
        Spacer(Modifier.height(bottomPadding))
    }
}

@Composable
private fun OwnProfileFallbackHeader(
    profile: OwnProfile, slots: OwnProfileSlots, callbacks: OwnProfileCallbacks, onPhoto: (String) -> Unit,
) {
    PublicProfileHeader(
        nickname = profile.userName, avatarUrl = profile.avatarUrl, about = profile.about, image = slots.shared.image,
        onAvatarClick = { profile.avatarUrl?.let(onPhoto) },
        stats = PublicProfileStats(profile.checkInCount, profile.addedShopsCount, profile.addedRoastersCount, profile.followersCount),
        onShopsClick = { callbacks.openContributions(ProfileContribution.Shops) },
        onRoastersClick = { callbacks.openContributions(ProfileContribution.Roasters) },
    )
}

@Composable
private fun OwnProfileFallbackCheckIns(count: Int, onOpen: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileSectionHeading(stringResource(Res.string.profile_section_check_ins), onOpen)
        if (count == 0) Text(stringResource(Res.string.profile_check_ins_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OwnProfileFallbackShelf(onEditProfile: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.profile_shelf_address_required), color = MaterialTheme.colorScheme.onSurfaceVariant)
        CoffeePeekTextButton(onEditProfile) { Text(stringResource(Res.string.profile_edit)) }
    }
}

@Composable
private fun OwnProfileFallbackPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val shared = ProfileSlots(image = { _, _, _ -> }, photoViewer = { _, _, _, _ -> }, photoPrefetch = { _, _ -> })
    val slots = OwnProfileSlots(shared, favoritesPreview = { _, _, header -> header() },
        activityCalendar = { _, _ -> }, activityPreview = { Text(stringResource(Res.string.profile_section_activity)) }, loadError = {})
    val callbacks = OwnProfileCallbacks({}, {}, {}, {}, { _, _, _ -> }, {}, {}, {}, {})
    val profile = OwnProfile("CoffeePeek", "preview@example.com", null, null, 0, 0, 0, 0, null)
    OwnProfileFallbackScreen(OwnProfileState(isLoggedIn = true, profile = profile, isLoading = false), slots, callbacks,
        actions = { OwnProfileActions({}, {}, {}, {}) }, bottomPadding = 20.dp, onRetry = {})
}

@Preview @Composable private fun OwnProfileFallbackLightPreview() = OwnProfileFallbackPreview(false)
@Preview @Composable private fun OwnProfileFallbackDarkPreview() = OwnProfileFallbackPreview(true)
