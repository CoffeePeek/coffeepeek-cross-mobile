package com.coffeepeek.admin.ui.dialogs

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformGlobalLoading(text: String?): Boolean = false

@Composable
internal actual fun PlatformGlobalError(title: String, message: String, closeLabel: String, onDismiss: () -> Unit): Boolean = false
