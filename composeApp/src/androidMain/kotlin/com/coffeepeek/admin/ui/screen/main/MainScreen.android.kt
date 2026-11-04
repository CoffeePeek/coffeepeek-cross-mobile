package com.coffeepeek.admin.ui.screen.main

import androidx.compose.runtime.Composable

@Composable
actual fun MainScreen(isLoggedIn: Boolean, onMapExpandedChange: (Boolean) -> Unit) =
    ComposeMainScreen(isLoggedIn, onMapExpandedChange)
