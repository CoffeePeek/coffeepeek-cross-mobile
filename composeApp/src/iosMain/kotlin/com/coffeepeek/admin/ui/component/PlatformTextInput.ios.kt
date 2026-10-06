@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PlatformImeOptions

actual fun platformTextInputOptions(options: KeyboardOptions): KeyboardOptions = options.copy(
    platformImeOptions = PlatformImeOptions { usingNativeTextInput(true) },
)
