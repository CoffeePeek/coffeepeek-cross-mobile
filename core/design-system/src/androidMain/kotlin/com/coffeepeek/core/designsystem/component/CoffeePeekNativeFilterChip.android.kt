package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal actual fun CoffeePeekNativeFilterChip(
    label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier,
    leadingIcon: ImageVector?, trailingIcon: ImageVector?,
): Boolean = false
