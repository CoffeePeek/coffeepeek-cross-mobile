package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun CoffeePeekNativeMultilineTextField(
    value: String, onValueChange: (String) -> Unit, label: String, placeholder: String,
    maxLength: Int, modifier: Modifier, enabled: Boolean, errorDescription: String?,
): Boolean = false
