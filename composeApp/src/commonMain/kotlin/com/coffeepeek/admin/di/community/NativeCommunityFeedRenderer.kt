package com.coffeepeek.admin.di.community

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.community.api.CommunityFeedActions
import com.coffeepeek.feature.community.api.CommunityFeedPresentation

internal typealias NativeCommunityFeedRenderer = @Composable (
    CommunityFeedPresentation, CommunityFeedActions, Modifier, @Composable () -> Unit, @Composable () -> Unit,
) -> Unit

internal expect fun nativeCommunityFeedRenderer(): NativeCommunityFeedRenderer?
