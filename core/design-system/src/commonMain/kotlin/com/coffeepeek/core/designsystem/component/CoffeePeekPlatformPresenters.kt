package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Seams for platform-native presentation. The app installs a presenter for a platform that has a native counterpart
 * (a UIKit sheet or navigation bar on iOS); a component asks it first and falls back to its own Compose rendering
 * when there is no presenter or the presenter reports that it did not take over.
 */
interface CoffeePeekNativeSheetPresenter {
    /** Presents [content] as a native sheet and returns true, or returns false to let the caller render its own sheet. */
    @Composable
    fun Present(
        onDismiss: () -> Unit,
        dismissEnabled: Boolean,
        maxSheetHeightFraction: Float?,
        content: @Composable ColumnScope.() -> Unit,
    ): Boolean
}

/** A header action is presentation state; its callback stays with the owning screen. */
data class CoffeePeekHeaderAction(val label: String, val onClick: () -> Unit)

interface CoffeePeekNativeHeaderPresenter {
    /** Shows the title, back and [action] in the native navigation bar and returns true, or false to let the caller draw its bar. */
    @Composable
    fun Present(title: String, onBack: (() -> Unit)?, customBack: Boolean, action: CoffeePeekHeaderAction?): Boolean
}

val LocalCoffeePeekNativeSheetPresenter = staticCompositionLocalOf<CoffeePeekNativeSheetPresenter?> { null }
val LocalCoffeePeekNativeHeaderPresenter = staticCompositionLocalOf<CoffeePeekNativeHeaderPresenter?> { null }
