package com.coffeepeek.feature.profile.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.profile.api.AccountDeletionEntry
import com.coffeepeek.feature.profile.api.AccountDeletionPort
import com.coffeepeek.feature.profile.api.EditProfileEntry
import com.coffeepeek.feature.profile.api.EditProfileSlots
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.impl.ui.AccountDeletionViewModel
import com.coffeepeek.feature.profile.impl.ui.EditProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.compose.AccountDeletionScreen
import com.coffeepeek.feature.profile.impl.ui.compose.EditProfileScreen

/**
 * The edit form. The ViewModel (form draft, picked avatar, save progress) lives in the host's ViewModel store, so it survives rotation and
 * theme changes; the caller owns navigation. The app supplies the own-account port, the writes and the picker/image slots.
 */
fun createEditProfileEntry(accounts: ProfileAccountPort, edit: ProfileEditPort, slots: EditProfileSlots): EditProfileEntry =
    object : EditProfileEntry {
        @Composable
        override fun Content(onBack: () -> Unit, onOpenAccountDeletion: () -> Unit, modifier: Modifier) {
            val model = viewModel(key = "edit-profile") { EditProfileViewModel(accounts, edit) }
            EditProfileScreen(model, slots, onBack, onOpenAccountDeletion, modifier)
        }
    }

/** The account deletion confirmation; see [AccountDeletionEntry]. */
fun createAccountDeletionEntry(accounts: ProfileAccountPort, deletion: AccountDeletionPort): AccountDeletionEntry =
    object : AccountDeletionEntry {
        @Composable
        override fun Content(onClose: () -> Unit, onSessionEnded: () -> Unit, modifier: Modifier) {
            val model = viewModel(key = "account-deletion") { AccountDeletionViewModel(accounts, deletion) }
            AccountDeletionScreen(model, onClose, onSessionEnded, modifier)
        }
    }
