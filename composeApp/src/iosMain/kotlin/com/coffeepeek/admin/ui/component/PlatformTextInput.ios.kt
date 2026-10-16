@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.input.KeyboardType
import platform.UIKit.UITextContentTypeName
import platform.UIKit.UITextContentTypeNewPassword
import platform.UIKit.UITextContentTypePassword
import platform.UIKit.UITextContentTypeUsername

// Stable option objects keep the UIKit input configuration unchanged while text recomposes.
private val nativeInput = PlatformImeOptions { usingNativeTextInput(true) }
private val secureInput = PlatformImeOptions { usingNativeTextInput(false) }
private val usernameInput = PlatformImeOptions { usingNativeTextInput(true); textContentType(UITextContentTypeUsername) }
private val nameInput = PlatformImeOptions { usingNativeTextInput(true); textContentType(UITextContentTypeName) }
private val passwordInput = PlatformImeOptions { usingNativeTextInput(false); textContentType(UITextContentTypePassword) }
private val newPasswordInput = PlatformImeOptions { usingNativeTextInput(false); textContentType(UITextContentTypeNewPassword) }

actual fun platformTextInputOptions(options: KeyboardOptions): KeyboardOptions = iosTextInputDefaults(options).copy(
    // Compose owns password masking and cursor state during visibility changes.
    // UIKit secure editing otherwise continues masking a revealed password.
    platformImeOptions = options.platformImeOptions ?: if (options.keyboardType == KeyboardType.Password ||
        options.keyboardType == KeyboardType.NumberPassword) secureInput else nativeInput,
)

internal actual fun platformAuthKeyboardOptions(options: KeyboardOptions, newPassword: Boolean): KeyboardOptions = options.copy(
    platformImeOptions = when {
        newPassword -> newPasswordInput
        options.keyboardType == KeyboardType.Password -> passwordInput
        options.keyboardType == KeyboardType.NumberPassword -> secureInput
        options.keyboardType == KeyboardType.Email -> usernameInput
        options.keyboardType == KeyboardType.Text -> nameInput
        else -> nativeInput
    },
)
