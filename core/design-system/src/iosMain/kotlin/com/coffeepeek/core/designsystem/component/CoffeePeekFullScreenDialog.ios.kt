package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
actual fun CoffeePeekFullScreenDialog(
    onDismiss: () -> Unit,
    darkSystemBarIcons: Boolean?,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            usePlatformInsets = false,
            // The full-screen form owns IME padding and reads its unconsumed visibility.
            useSoftwareKeyboardInset = false,
            scrimColor = Color.Transparent,
        ),
        content = content,
    )
}
