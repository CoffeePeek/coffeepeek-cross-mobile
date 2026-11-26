package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.*

internal val LocalNativeHomeFullScreen = staticCompositionLocalOf { false }

@Composable
internal actual fun PlatformHomeCheckInFullScreen(content: @Composable () -> Unit): Boolean {
    CompositionLocalProvider(LocalNativeHomeFullScreen provides true, content = content)
    return true
}
