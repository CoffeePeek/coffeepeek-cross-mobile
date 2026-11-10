package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable

/**
 * Edge-to-edge modal. Content paints behind the bars and reserves insets for controls.
 * [darkSystemBarIcons] sets the Android dialog window's icon appearance.
 */
@Composable
expect fun CoffeePeekFullScreenDialog(
    onDismiss: () -> Unit,
    darkSystemBarIcons: Boolean? = null,
    content: @Composable () -> Unit,
)
