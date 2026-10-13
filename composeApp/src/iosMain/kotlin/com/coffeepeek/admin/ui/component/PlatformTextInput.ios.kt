@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.input.KeyboardType

actual fun platformTextInputOptions(options: KeyboardOptions): KeyboardOptions = options.copy(
    // Compose owns password masking and cursor state during visibility changes.
    // UIKit secure editing otherwise continues masking a revealed password.
    platformImeOptions = PlatformImeOptions {
        usingNativeTextInput(options.keyboardType !in setOf(
            KeyboardType.Password, KeyboardType.NumberPassword,
        ))
    },
)
