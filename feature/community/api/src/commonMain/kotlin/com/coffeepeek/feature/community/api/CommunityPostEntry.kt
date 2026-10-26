package com.coffeepeek.feature.community.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * One post, as a shared link (`https://coffeepeek.by/check-ins/{id}`) opens it. The caller owns navigation and the ViewModel store;
 * a post the viewer may not see shows an «unavailable» state rather than an error.
 */
interface CommunityPostEntry {
    @Composable
    fun Content(
        postId: String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenAuthor: (slug: String, name: String, avatarUrl: String?) -> Unit,
        onOpenShop: (slug: String) -> Unit,
        modifier: Modifier,
    )
}
