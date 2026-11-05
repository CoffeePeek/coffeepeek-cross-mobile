package com.coffeepeek.admin.feature.community.ui

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface CommunityScreenRenderer {
    @Composable
    fun Content(onCreateCheckIn: () -> Unit, notificationsAction: @Composable () -> Unit)
}
