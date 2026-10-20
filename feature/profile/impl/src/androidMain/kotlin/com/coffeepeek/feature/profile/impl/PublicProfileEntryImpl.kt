package com.coffeepeek.feature.profile.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.api.ProfileAuthorVisibility
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.api.PublicProfileEntry
import com.coffeepeek.feature.profile.domain.PublicProfileData
import com.coffeepeek.feature.profile.impl.ui.PublicProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.compose.PublicProfileScreen
import java.util.UUID

/**
 * The caller owns destination lifetime (destroy its ViewModelStore on removal), root navigation, sign-in and sharing; the app supplies
 * the account, author visibility, author actions and the legacy-rendered pieces ([ProfileSlots]). No network client is created here.
 */
fun createPublicProfileEntry(
    data: PublicProfileData,
    checkIns: PublicCheckInsPort,
    accounts: ProfileAccountPort,
    visibility: ProfileAuthorVisibility = ProfileAuthorVisibility.None,
    authorActions: ProfileAuthorActions = ProfileAuthorActions.None,
    slots: ProfileSlots,
): PublicProfileEntry = PublicProfileEntryImpl(data, checkIns, accounts, visibility, authorActions, slots)

private class PublicProfileEntryImpl(
    private val data: PublicProfileData,
    private val checkIns: PublicCheckInsPort,
    private val accounts: ProfileAccountPort,
    private val visibility: ProfileAuthorVisibility,
    private val authorActions: ProfileAuthorActions,
    private val slots: ProfileSlots,
) : PublicProfileEntry {
    private val instanceKey = UUID.randomUUID().toString()

    @Composable
    override fun Content(
        slug: String, nickname: String?, avatarUrl: String?, onBack: () -> Unit, onOpenShop: (String) -> Unit,
        onOpenRoaster: (String) -> Unit, onLogin: () -> Unit, onOpenUser: (String, String, String?) -> Unit,
        onShare: (String) -> Unit, modifier: Modifier,
    ) {
        val model = viewModel(key = "public-profile:$instanceKey:$slug") { PublicProfileViewModel(slug, data, checkIns, accounts, visibility) }
        PublicProfileScreen(slug, nickname, avatarUrl, model, authorActions, slots, onBack, onOpenShop, onOpenRoaster, onLogin, onOpenUser, onShare)
    }
}
