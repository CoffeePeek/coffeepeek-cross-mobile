package com.coffeepeek.admin.di.community

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.community.api.CommunityCardActions
import com.coffeepeek.feature.community.api.CommunityCardPresentation

internal typealias NativeCommunityCardRenderer = @Composable (CommunityCardPresentation, CommunityCardActions, Modifier) -> Unit

internal expect fun nativeCommunityCardRenderer(): NativeCommunityCardRenderer?
