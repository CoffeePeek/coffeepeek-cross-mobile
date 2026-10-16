package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/** The app boundary owns platform navigation; feature UI and routes remain shared. */
@Composable
internal expect fun PlatformRootNavigation(
    isLoggedIn: Boolean,
    events: Flow<Navigator.NavEvent>,
    appLink: Navigator.Screen?,
    consumeLink: () -> Unit,
    content: @Composable (Navigator.Screen, resetToLogin: () -> Unit) -> Unit,
): Boolean

/** Called only when the authenticated account changes. */
internal expect fun clearPlatformNavigationState()
