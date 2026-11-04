package com.coffeepeek.admin.ui.screen.main

import androidx.compose.runtime.Composable

/** Legacy renderer for the shared root entry; PlatformRootNavigation owns the live iOS tabs. */
@Composable
actual fun MainScreen(isLoggedIn: Boolean, onMapExpandedChange: (Boolean) -> Unit) =
    ComposeMainScreen(isLoggedIn, onMapExpandedChange)
