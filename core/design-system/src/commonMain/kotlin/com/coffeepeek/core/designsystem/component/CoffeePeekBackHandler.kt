package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable

/**
 * Intercepts the system back action while [enabled]. Android uses the activity's back dispatcher; iOS leaves back to the native
 * navigation controller (its edge swipe and bar), so the handler does nothing there.
 */
@Composable
expect fun CoffeePeekBackHandler(enabled: Boolean = true, onBack: () -> Unit)
