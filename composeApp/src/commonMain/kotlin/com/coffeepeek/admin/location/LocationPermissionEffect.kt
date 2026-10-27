package com.coffeepeek.admin.location

import androidx.compose.runtime.Composable

@Composable
expect fun LocationPermissionEffect(
    requestKey: Int,
    onGranted: () -> Unit,
    onDenied: () -> Unit = {},
    requestOnce: Boolean = false,
)
