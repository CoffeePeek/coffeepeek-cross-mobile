package com.coffeepeek.core.designsystem.component

import android.graphics.Color
import android.view.Window
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable
actual fun CoffeePeekFullScreenDialog(
    onDismiss: () -> Unit,
    darkSystemBarIcons: Boolean?,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        // Full-screen content owns its background, including any dismissal scrim.
        SideEffect {
            window.setDimAmount(0f)
            if (darkSystemBarIcons != null) window.applyCoffeePeekDialogSystemBars(darkSystemBarIcons)
        }
        content()
    }
}

@Suppress("DEPRECATION") // Transparent bar colors are needed on Android versions before edge-to-edge enforcement.
private fun Window.applyCoffeePeekDialogSystemBars(darkIcons: Boolean) {
    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT
    isStatusBarContrastEnforced = false
    isNavigationBarContrastEnforced = false
    val mask = APPEARANCE_LIGHT_STATUS_BARS or APPEARANCE_LIGHT_NAVIGATION_BARS
    insetsController?.setSystemBarsAppearance(if (darkIcons) mask else 0, mask)
}
