package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun CoffeePeekNativePrimaryButton(
    text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean,
): Boolean
