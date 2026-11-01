package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A native field owns its material and text together so the material never blurs the text. */
@Composable
internal expect fun CoffeePeekPlatformSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
    enabled: Boolean,
    readOnly: Boolean,
    modifier: Modifier,
    keyboardOptions: KeyboardOptions,
    onSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
): Boolean
