package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CoffeePeekNativeModalSheet(
    onDismiss: () -> Unit, dismissEnabled: Boolean, maxHeightFraction: Float?,
    modifier: Modifier, content: @Composable ColumnScope.() -> Unit,
): Boolean = false
