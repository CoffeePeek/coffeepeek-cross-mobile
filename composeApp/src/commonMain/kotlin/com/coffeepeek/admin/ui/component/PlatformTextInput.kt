package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions

/** Keep the field's IME configuration while opting iOS into UIKit text editing. */
expect fun platformTextInputOptions(options: KeyboardOptions = KeyboardOptions.Default): KeyboardOptions
