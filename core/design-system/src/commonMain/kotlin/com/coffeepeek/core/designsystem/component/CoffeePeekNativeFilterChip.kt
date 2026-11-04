package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

@Composable
internal expect fun CoffeePeekNativeFilterChip(
    label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier,
    leadingIcon: Painter?, trailingIcon: Painter?,
): Boolean
