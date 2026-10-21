package com.coffeepeek.core.designsystem.component

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncedTextFieldStateTest {
    @Test fun delayedEchoDoesNotRewindNewerTypingOrComposition() {
        val field = SyncedTextFieldState("")
        field.value = TextFieldValue("к", TextRange(1))
        field.value = TextFieldValue("ко", TextRange(2), composition = TextRange(0, 2))
        field.syncExternal("к") // the first publish comes back after the second keystroke
        assertEquals("ко", field.value.text)
        assertEquals(TextRange(0, 2), field.value.composition)
        field.syncExternal("ко")
        assertEquals("ко", field.value.text)
    }

    @Test fun distinctExternalReplacementResetsTheField() {
        val field = SyncedTextFieldState("кофе")
        field.syncExternal("")
        assertEquals("", field.value.text)
    }
}
