package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import kotlin.test.Test
import kotlin.test.assertEquals

class IosTextInputDefaultsTest {
    @Test fun notesNamesAndSearchGetSentenceCapitalizationAndSuggestions() {
        val options = iosTextInputDefaults(KeyboardOptions(imeAction = ImeAction.Search))
        assertEquals(true, options.autoCorrectEnabled)
        assertEquals(KeyboardCapitalization.Sentences, options.capitalization)
        assertEquals(ImeAction.Search, options.imeAction)
    }

    @Test fun explicitWordCapitalizationAndFocusActionArePreserved() {
        val options = iosTextInputDefaults(KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
        assertEquals(KeyboardCapitalization.Words, options.capitalization)
        assertEquals(true, options.autoCorrectEnabled)
        assertEquals(ImeAction.Next, options.imeAction)
    }

    @Test fun literalValuesDoNotGetTextCorrections() {
        for (type in listOf(KeyboardType.Email, KeyboardType.Uri, KeyboardType.Ascii, KeyboardType.Password,
            KeyboardType.NumberPassword, KeyboardType.Phone, KeyboardType.Number, KeyboardType.Decimal)) {
            val options = iosTextInputDefaults(KeyboardOptions(keyboardType = type))
            assertEquals(type, options.keyboardType)
            assertEquals(false, options.autoCorrectEnabled)
            assertEquals(KeyboardCapitalization.None, options.capitalization)
        }
    }

    @Test fun explicitCredentialOptOutIsPreserved() {
        val options = iosTextInputDefaults(KeyboardOptions(autoCorrectEnabled = false))
        assertEquals(false, options.autoCorrectEnabled)
        assertEquals(KeyboardCapitalization.None, options.capitalization)
    }

    @Test fun explicitCapitalizationOptOutIsPreserved() {
        val options = iosTextInputDefaults(KeyboardOptions(capitalization = KeyboardCapitalization.None))
        assertEquals(true, options.autoCorrectEnabled)
        assertEquals(KeyboardCapitalization.None, options.capitalization)
    }
}
