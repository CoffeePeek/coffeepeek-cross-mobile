package com.coffeepeek.admin.ui.component

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TextInputStateTest {
    @Test fun delayedViewModelEchoDoesNotRollBackTypingOrMarkedText() {
        val field = SyncedTextFieldState("")
        field.value = TextFieldValue("к", TextRange(1), TextRange(0, 1))
        field.value = TextFieldValue("кофе", TextRange(4), TextRange(0, 4))
        val latest = field.value
        field.syncExternal("к")
        assertSame(latest, field.value)
        field.syncExternal("кофе")
        assertSame(latest, field.value)
    }

    @Test fun coalescedEchoPreservesSelectionAndAutocorrection() {
        val field = SyncedTextFieldState("")
        field.value = TextFieldValue("капучино", TextRange(8), TextRange(0, 8))
        field.value = TextFieldValue("капучино вкусный", TextRange(9, 16), TextRange(9, 16))
        val corrected = field.value
        field.syncExternal(corrected.text)
        assertSame(corrected, field.value)
    }

    @Test fun repeatedTextDuringBackspaceCanAcknowledgeEarlierEdits() {
        val field = SyncedTextFieldState("")
        field.value = TextFieldValue("a", TextRange(1))
        field.value = TextFieldValue("ab", TextRange(2))
        field.value = TextFieldValue("a", TextRange(1))
        field.syncExternal("a")
        field.syncExternal("ab")
        assertEquals("a", field.value.text)
        field.syncExternal("a")
        assertEquals(TextRange(1), field.value.selection)
    }

    @Test fun externallyLoadedDraftAndResetStillReplaceTheText() {
        val field = SyncedTextFieldState("old")
        field.value = TextFieldValue("old text", TextRange(8), TextRange(4, 8))
        field.syncExternal("loaded draft")
        assertEquals("loaded draft", field.value.text)
        assertEquals(TextRange(8), field.value.selection)
        assertNull(field.value.composition)
        field.syncExternal("")
        assertEquals("", field.value.text)
        assertEquals(TextRange(0), field.value.selection)
    }

    @Test fun cursorAndCompositionChangesDoNotRepublishTheForm() {
        val field = SyncedTextFieldState("coffee")
        val published = mutableListOf<String>()
        val update = TextFieldValue("coffee", TextRange(2), TextRange(0, 6))
        field.updateFromInput(update, published::add)
        assertTrue(published.isEmpty())
        assertSame(update, field.value)
        field.updateFromInput(TextFieldValue("coffees", TextRange(7)), published::add)
        assertEquals(listOf("coffees"), published)
    }

    @Test fun lengthLimitPreservesValidCompositionAndSelection() {
        val limited = TextFieldValue("coffee!", TextRange(7), TextRange(0, 6)).limitTextLength(6)
        assertEquals("coffee", limited.text)
        assertEquals(TextRange(6), limited.selection)
        assertEquals(TextRange(0, 6), limited.composition)
    }

    @Test fun lengthLimitDoesNotSplitEmoji() {
        val limited = TextFieldValue("a☕\uD83D\uDE00", TextRange(4)).limitTextLength(3)
        assertEquals("a☕", limited.text)
        assertEquals(TextRange(2), limited.selection)
    }

    @Test fun numericFilteringMapsTheCursorAndComposition() {
        val filtered = TextFieldValue("1a23", TextRange(3, 1), TextRange(1, 4)).filterText(Char::isDigit)
        assertEquals("123", filtered.text)
        assertEquals(TextRange(2, 1), filtered.selection)
        assertEquals(TextRange(1, 3), filtered.composition)
    }

    @Test fun rejectedNumericEditCannotDisplayADifferentValueThanTheDraft() {
        val field = SyncedTextFieldState("5")
        val published = mutableListOf<String>()
        field.updateFromInput(TextFieldValue("59", TextRange(2)), published::add) { it.toIntOrNull() in 0..59 }
        assertEquals(listOf("59"), published)
        field.syncExternal("59")
        val accepted = field.value
        field.updateFromInput(TextFieldValue("599", TextRange(3)), published::add) { it.toIntOrNull() in 0..59 }
        assertSame(accepted, field.value)
        assertEquals(listOf("59"), published)
    }
}
