package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun PlatformScrollableColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!NativeScrollColumn(modifier, verticalArrangement, horizontalAlignment, content)) {
        Column(modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement, horizontalAlignment, content)
    }
}

@Composable
internal expect fun NativeScrollColumn(modifier: Modifier, arrangement: Arrangement.Vertical, alignment: Alignment.Horizontal, content: @Composable ColumnScope.() -> Unit): Boolean
