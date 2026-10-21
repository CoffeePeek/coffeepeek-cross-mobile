package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.screen.deleteaccount.DeleteAccountPendingScreen
import com.coffeepeek.admin.ui.screen.editprofile.EditProfileScreen

/** Platform selection only. Shared/iOS keep the legacy edit and deletion screens; Android composes the migrated entries. */
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
