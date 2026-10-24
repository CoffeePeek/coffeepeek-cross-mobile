package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.LocalCoffeePeekScreenInsets

@Composable
internal fun PlatformScrollableColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    reserveBottomClearance: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val insets = LocalCoffeePeekScreenInsets.current
    val bottomClearance = if (reserveBottomClearance && insets.extendsToBottom) insets.bottom else 0.dp
    val body: @Composable ColumnScope.() -> Unit = {
        content()
        if (bottomClearance > 0.dp) Spacer(Modifier.height(bottomClearance))
    }
    if (!NativeScrollColumn(modifier, verticalArrangement, horizontalAlignment, body)) {
        Column(modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement, horizontalAlignment, body)
    }
}

@Composable
internal expect fun NativeScrollColumn(modifier: Modifier, arrangement: Arrangement.Vertical, alignment: Alignment.Horizontal, content: @Composable ColumnScope.() -> Unit): Boolean
