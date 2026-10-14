@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.input.KeyboardType
import platform.UIKit.UITextContentTypeName
import platform.UIKit.UITextContentTypeNewPassword
import platform.UIKit.UITextContentTypePassword
import platform.UIKit.UITextContentTypeUsername

actual fun platformTextInputOptions(options: KeyboardOptions): KeyboardOptions = options.copy(
    // Compose owns password masking and cursor state during visibility changes.
    // UIKit secure editing otherwise continues masking a revealed password.
    platformImeOptions = options.platformImeOptions ?: PlatformImeOptions {
        usingNativeTextInput(options.keyboardType !in setOf(
            KeyboardType.Password, KeyboardType.NumberPassword,
        ))
    },
)

internal actual fun platformAuthKeyboardOptions(options: KeyboardOptions, newPassword: Boolean): KeyboardOptions = options.copy(
    platformImeOptions = PlatformImeOptions {
        usingNativeTextInput(options.keyboardType !in setOf(KeyboardType.Password, KeyboardType.NumberPassword))
        textContentType(when {
            newPassword -> UITextContentTypeNewPassword
            options.keyboardType == KeyboardType.Password -> UITextContentTypePassword
            options.keyboardType == KeyboardType.Email -> UITextContentTypeUsername
            options.keyboardType == KeyboardType.Text -> UITextContentTypeName
            else -> null
        })
    },
)
