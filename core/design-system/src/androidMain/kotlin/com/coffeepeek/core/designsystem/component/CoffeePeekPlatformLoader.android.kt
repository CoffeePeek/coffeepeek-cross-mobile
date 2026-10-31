package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
internal actual fun CoffeePeekPlatformLoader(
    contentDescription: String, modifier: Modifier, size: Dp, color: Color, strokeWidth: Dp,
) = CoffeePeekMorphingLoader(contentDescription, modifier, size, color, strokeWidth)
