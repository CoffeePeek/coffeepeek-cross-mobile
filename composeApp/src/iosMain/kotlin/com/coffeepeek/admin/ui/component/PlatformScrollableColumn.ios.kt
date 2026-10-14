package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Nested UIKit scrolling leaves an opaque rectangle in Compose cards and sheets.
@Composable
internal actual fun NativeScrollColumn(
    modifier: Modifier,
    arrangement: Arrangement.Vertical,
    alignment: Alignment.Horizontal,
    content: @Composable ColumnScope.() -> Unit,
): Boolean = false
