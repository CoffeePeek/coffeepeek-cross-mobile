package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens

@Composable
internal expect fun CoffeePeekPlatformSegments(
    labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier, enabled: Boolean,
    minSegmentHeight: Dp = CoffeePeekDimens.buttonHeight,
)
