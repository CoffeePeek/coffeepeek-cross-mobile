package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions

/** Keep IME configuration; iOS auth email and password fields use Compose-managed editing. */
expect fun platformTextInputOptions(options: KeyboardOptions = KeyboardOptions.Default): KeyboardOptions

/** Credential roles are configured by the platform while Compose owns the field's appearance. */
internal expect fun platformAuthKeyboardOptions(options: KeyboardOptions, newPassword: Boolean): KeyboardOptions
