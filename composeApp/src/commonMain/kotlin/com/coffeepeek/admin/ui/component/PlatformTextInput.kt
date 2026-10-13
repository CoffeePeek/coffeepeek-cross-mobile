package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions

/** Keep IME configuration; iOS uses UIKit editing except for Compose-managed passwords. */
expect fun platformTextInputOptions(options: KeyboardOptions = KeyboardOptions.Default): KeyboardOptions
