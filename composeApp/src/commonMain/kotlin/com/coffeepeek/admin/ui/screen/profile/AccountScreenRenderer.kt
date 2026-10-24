package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.screen.deleteaccount.DeleteAccountPendingScreen
import com.coffeepeek.admin.ui.screen.editprofile.EditProfileScreen

/** Both platforms bind feature edit/deletion entries; the legacy screens remain a fallback. */
interface AccountScreenRenderer {
    @Composable
    fun EditProfile()

    @Composable
    fun DeletionPending()
}

internal object LegacyAccountScreenRenderer : AccountScreenRenderer {
    @Composable
    override fun EditProfile() = EditProfileScreen()

    @Composable
    override fun DeletionPending() = DeleteAccountPendingScreen()
}
