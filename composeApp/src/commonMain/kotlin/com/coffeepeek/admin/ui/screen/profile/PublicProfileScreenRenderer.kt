package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface PublicProfileScreenRenderer {
    @Composable
    fun Content(slug: String, nickname: String?, avatarUrl: String?, checkInsOnly: Boolean)
}
