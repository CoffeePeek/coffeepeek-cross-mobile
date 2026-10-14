package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable

/** A header action is presentation state; its callback stays with the owning screen. */
data class NavigationBarAction(val label: String, val onClick: () -> Unit)

@Composable
internal expect fun NativeNavigationBar(
    title: String,
    onBack: (() -> Unit)?,
    customBack: Boolean,
    action: NavigationBarAction?,
): Boolean
