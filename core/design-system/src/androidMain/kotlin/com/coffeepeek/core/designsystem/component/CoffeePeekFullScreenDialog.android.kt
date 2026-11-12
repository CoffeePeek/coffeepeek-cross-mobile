package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

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
        val view = LocalView.current
        val window = (view.parent as DialogWindowProvider).window
        SideEffect(window, darkSystemBarIcons) {
            val enforceNavigationBarContrast = window.isNavigationBarContrastEnforced
            WindowCompat.enableEdgeToEdge(window)
            // Full-screen content draws its own background and dismissal scrim.
            window.setDimAmount(0f)
            if (darkSystemBarIcons == null) {
                // Keep the window's default protection for three-button navigation.
                window.isNavigationBarContrastEnforced = enforceNavigationBarContrast
            } else {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = darkSystemBarIcons
                    isAppearanceLightNavigationBars = darkSystemBarIcons
                }
            }
        }
        content()
    }
}
