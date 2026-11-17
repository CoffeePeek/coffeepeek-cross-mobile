package com.coffeepeek.admin.di.community

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.homecheckin.api.HomeCheckInNativeDetailRenderer

internal actual fun nativeCommunityDetailRenderer(): NativeCommunityDetailRenderer? = null
internal actual fun nativeHomeDetailRenderer(): HomeCheckInNativeDetailRenderer? = null
@Composable internal actual fun nativeCheckInDetailAvailable(): Boolean = false
