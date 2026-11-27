package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

// The native application starts IosNativeRoot directly; shared navigation is Android's adapter.
internal actual fun clearPlatformNavigationState() = Unit

@Composable
internal actual fun PlatformRootNavigation(isLoggedIn: Boolean, events: Flow<Navigator.NavEvent>,
    appLink: Navigator.Screen?, consumeLink: () -> Unit,
    content: @Composable (Navigator.Screen, resetToLogin: () -> Unit) -> Unit,
): Boolean = error("iOS starts native root navigation directly")
