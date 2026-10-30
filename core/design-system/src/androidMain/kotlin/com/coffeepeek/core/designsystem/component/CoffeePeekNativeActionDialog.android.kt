package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable

@Composable
internal actual fun CoffeePeekNativeActionDialog(
    title: String, message: String?, actions: List<CoffeePeekDialogAction>, cancelLabel: String?, onDismiss: () -> Unit,
): Boolean = false
