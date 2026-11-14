package com.coffeepeek.admin.location

import androidx.compose.runtime.Composable

@Composable
expect fun LocationPermissionEffect(
    requestKey: Int,
    onGranted: () -> Unit,
    onDenied: () -> Unit = {},
    /** The permission cannot be granted without leaving the app; the notice must offer the way to settings. */
    onPermanentlyDenied: () -> Unit = {},
    requestOnce: Boolean = false,
)
