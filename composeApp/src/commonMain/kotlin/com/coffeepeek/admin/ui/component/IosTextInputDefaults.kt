package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

/** UIKit traits for prose versus literal values; Android keeps its existing options. */
internal fun iosTextInputDefaults(options: KeyboardOptions): KeyboardOptions {
    val prose = options.keyboardType == KeyboardType.Text || options.keyboardType == KeyboardType.Unspecified
    val autocorrect = options.autoCorrectEnabled ?: prose
    return options.copy(
        autoCorrectEnabled = autocorrect,
        capitalization = if (options.capitalization == KeyboardCapitalization.Unspecified) {
            if (prose && autocorrect) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
        } else options.capitalization,
    )
}
