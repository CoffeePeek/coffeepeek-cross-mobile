package com.coffeepeek.admin.feature.community.ui

import androidx.compose.runtime.Composable

/** Platform selection only. Android and iOS bind the shared feature renderer; the default remains a legacy fallback. */
interface CommunityScreenRenderer {
    @Composable
    fun Content(onCreateCheckIn: () -> Unit, notificationsAction: @Composable () -> Unit)
}

internal object LegacyCommunityScreenRenderer : CommunityScreenRenderer {
    @Composable
    override fun Content(onCreateCheckIn: () -> Unit, notificationsAction: @Composable () -> Unit) = CommunityScreen(onCreateCheckIn, notificationsAction)
}
