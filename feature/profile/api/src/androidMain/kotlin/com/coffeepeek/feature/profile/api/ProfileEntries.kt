package com.coffeepeek.feature.profile.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Another author's public profile (or the viewer's own through an alias). Caller owns navigation, sign-in and sharing. */
interface PublicProfileEntry {
    @Composable
    fun Content(
        slug: String,
        nickname: String?,
        avatarUrl: String?,
        onBack: () -> Unit,
        onOpenShop: (slug: String) -> Unit,
        onOpenRoaster: (slug: String) -> Unit,
        onLogin: () -> Unit,
        onOpenUser: (slug: String, nickname: String, avatarUrl: String?) -> Unit,
        /** Receives the full public profile URL. */
        onShare: (url: String) -> Unit,
        onOpenCheckIns: () -> Unit,
        checkInsOnly: Boolean = false,
        modifier: Modifier = Modifier,
    )
}
