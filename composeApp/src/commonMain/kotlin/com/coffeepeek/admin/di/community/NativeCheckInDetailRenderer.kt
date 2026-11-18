package com.coffeepeek.admin.di.community

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.community.api.CommunityCardActions
import com.coffeepeek.feature.community.api.CommunityPostPresentation
import com.coffeepeek.feature.homecheckin.api.HomeCheckInNativeDetailRenderer

internal typealias NativeCommunityDetailRenderer = @Composable (CommunityPostPresentation, CommunityCardActions, Modifier, @Composable () -> Unit) -> Boolean
internal expect fun nativeCommunityDetailRenderer(): NativeCommunityDetailRenderer?
internal expect fun nativeHomeDetailRenderer(): HomeCheckInNativeDetailRenderer?
@Composable internal expect fun nativeCheckInDetailAvailable(): Boolean
