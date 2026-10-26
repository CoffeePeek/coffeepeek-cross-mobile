package com.coffeepeek.admin.feature.community.ui

import androidx.compose.runtime.Composable

/** Platform selection only. iOS and shared code keep the legacy feed; Android composes the migrated entry. */
interface CommunityScreenRenderer {
    @Composable
    fun Content(onCreateCheckIn: () -> Unit, notificationsAction: @Composable () -> Unit)
}

internal object LegacyCommunityScreenRenderer : CommunityScreenRenderer {
    @Composable
    override fun Content(onCreateCheckIn: () -> Unit, notificationsAction: @Composable () -> Unit) = CommunityScreen(onCreateCheckIn, notificationsAction)
}
