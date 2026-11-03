package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun CoffeePeekNativeMenu(
    actions: List<CoffeePeekMenuAction>, modifier: Modifier, enabled: Boolean,
    content: @Composable (open: () -> Unit) -> Unit,
): Boolean
