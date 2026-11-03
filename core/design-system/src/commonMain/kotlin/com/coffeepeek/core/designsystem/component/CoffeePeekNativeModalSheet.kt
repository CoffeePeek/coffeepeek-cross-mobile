package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

internal val LocalCoffeePeekSheetExpandToMaxHeight = staticCompositionLocalOf { false }

@Composable
internal fun CoffeePeekNativeModalSheetPresentation(
    onDismiss: () -> Unit, dismissEnabled: Boolean, maxHeightFraction: Float?,
    modifier: Modifier, expandToMaxHeight: Boolean, content: @Composable ColumnScope.() -> Unit,
): Boolean {
    var handled = false
    CompositionLocalProvider(LocalCoffeePeekSheetExpandToMaxHeight provides expandToMaxHeight) {
        handled = CoffeePeekNativeModalSheet(onDismiss, dismissEnabled, maxHeightFraction, modifier, content)
    }
    return handled
}

@Composable
expect fun CoffeePeekNativeModalSheet(
    onDismiss: () -> Unit, dismissEnabled: Boolean, maxHeightFraction: Float?,
    modifier: Modifier, content: @Composable ColumnScope.() -> Unit,
): Boolean
