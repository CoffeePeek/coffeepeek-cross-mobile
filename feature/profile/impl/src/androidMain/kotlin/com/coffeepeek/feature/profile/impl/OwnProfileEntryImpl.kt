package com.coffeepeek.feature.profile.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.profile.api.OwnProfileEntry
import com.coffeepeek.feature.profile.api.OwnProfileSlots
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.api.ProfileSignOutPort
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.domain.PublicProfileData
import com.coffeepeek.feature.profile.impl.ui.OwnProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.PublicProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.compose.OwnProfileScreen
import java.util.UUID

/**
 * The Profile tab. Caller owns the tab's ViewModel-store lifetime, navigation, sign-in and the floating bar clearance; the app supplies
 * the account, sign-out, author visibility and actions, and the legacy-rendered pieces ([OwnProfileSlots]).
 */
fun createOwnProfileEntry(
    data: PublicProfileData,
    checkIns: PublicCheckInsPort,
    accounts: ProfileAccountPort,
    signOut: ProfileSignOutPort,
    visibility: ProfileAuthorVisibility = ProfileAuthorVisibility.None,
    authorActions: ProfileAuthorActions = ProfileAuthorActions.None,
    slots: OwnProfileSlots,
): OwnProfileEntry = OwnProfileEntryImpl(data, checkIns, accounts, signOut, visibility, authorActions, slots)

private class OwnProfileEntryImpl(
    private val data: PublicProfileData,
    private val checkIns: PublicCheckInsPort,
    private val accounts: ProfileAccountPort,
    private val signOut: ProfileSignOutPort,
    private val visibility: ProfileAuthorVisibility,
    private val authorActions: ProfileAuthorActions,
    private val slots: OwnProfileSlots,
) : OwnProfileEntry {
    private val instanceKey = UUID.randomUUID().toString()

    @Composable
    override fun Content(
        onEditProfile: () -> Unit, onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit, onLogin: () -> Unit,
        onOpenUser: (String, String, String?) -> Unit, onOpenFavorites: () -> Unit, onOpenCheckIns: () -> Unit,
        onOpenContributions: (ProfileContribution) -> Unit, onOpenBlockedAuthors: () -> Unit, modifier: Modifier, bottomClearance: Dp,
    ) {
        val model = viewModel(key = "own-profile:$instanceKey") { OwnProfileViewModel(accounts, signOut) }
        OwnProfileScreen(
            vm = model,
            publicModel = { slug -> viewModel(key = "own-public:$instanceKey:$slug") { PublicProfileViewModel(slug, data, checkIns, accounts, visibility) } },
            authorActions = authorActions, slots = slots, bottomClearance = bottomClearance,
            onEditProfile = onEditProfile, onOpenShop = onOpenShop, onOpenRoaster = onOpenRoaster, onLogin = onLogin,
            onOpenUser = onOpenUser, onOpenFavorites = onOpenFavorites, onOpenCheckIns = onOpenCheckIns,
            onOpenContributions = onOpenContributions,
            onOpenBlockedAuthors = onOpenBlockedAuthors,

        )
    }
}
