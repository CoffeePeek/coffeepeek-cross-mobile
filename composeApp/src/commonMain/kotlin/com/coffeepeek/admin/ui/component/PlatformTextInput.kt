package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions

/** Keep IME configuration; iOS uses UIKit editing except for Compose-managed passwords. */
expect fun platformTextInputOptions(options: KeyboardOptions = KeyboardOptions.Default): KeyboardOptions

/** Credential roles are configured by the platform while Compose owns the field's appearance. */
internal expect fun platformAuthKeyboardOptions(options: KeyboardOptions, newPassword: Boolean): KeyboardOptions
