@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
actual fun CoffeePeekFullScreenDialog(
    onDismiss: () -> Unit,
    darkSystemBarIcons: Boolean?,
    content: @Composable () -> Unit,
) {
    val host = LocalUIViewController.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            usePlatformInsets = false,
            // The full-screen form owns IME padding and reads its unconsumed visibility.
            useSoftwareKeyboardInset = false,
            scrimColor = Color.Transparent,
        ),
    ) {
        // UIKit can detach the Compose dialog layer when it presents another controller (CMP-10589).
        CompositionLocalProvider(LocalUIViewController provides host, content = content)
    }
}
