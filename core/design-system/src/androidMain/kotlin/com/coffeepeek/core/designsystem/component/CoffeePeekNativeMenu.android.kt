package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun CoffeePeekNativeMenu(
    actions: List<CoffeePeekMenuAction>, modifier: Modifier, content: @Composable (open: () -> Unit) -> Unit,
): Boolean = false
