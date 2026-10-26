package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable

/** Edge-to-edge modal. Content paints the background and reserves safe insets for controls. */
@Composable
internal expect fun PlatformFullScreenDialog(onDismiss: () -> Unit, content: @Composable () -> Unit)
