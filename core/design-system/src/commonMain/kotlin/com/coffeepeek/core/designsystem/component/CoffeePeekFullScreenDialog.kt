package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable

/** Edge-to-edge modal. Content paints the background and reserves safe insets for controls. */
@Composable
expect fun CoffeePeekFullScreenDialog(onDismiss: () -> Unit, content: @Composable () -> Unit)
