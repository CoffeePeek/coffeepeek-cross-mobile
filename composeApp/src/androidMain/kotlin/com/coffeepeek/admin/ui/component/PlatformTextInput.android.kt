package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions

actual fun platformTextInputOptions(options: KeyboardOptions): KeyboardOptions = options

internal actual fun platformAuthKeyboardOptions(options: KeyboardOptions, newPassword: Boolean): KeyboardOptions = options
