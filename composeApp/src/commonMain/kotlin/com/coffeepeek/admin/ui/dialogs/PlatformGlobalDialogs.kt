package com.coffeepeek.admin.ui.dialogs

import androidx.compose.runtime.Composable

@Composable
internal expect fun PlatformGlobalLoading(text: String?): Boolean

@Composable
internal expect fun PlatformGlobalError(title: String, message: String, closeLabel: String, onDismiss: () -> Unit): Boolean
