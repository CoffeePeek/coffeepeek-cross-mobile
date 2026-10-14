package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal actual fun NativeScrollColumn(modifier: Modifier, arrangement: Arrangement.Vertical, alignment: Alignment.Horizontal, content: @Composable ColumnScope.() -> Unit) = false
