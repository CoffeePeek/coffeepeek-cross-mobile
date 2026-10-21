package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Keeps cursor and IME composition local while a ViewModel publishes the edited string asynchronously.
 * Local edits win over delayed echoes (so autocorrect and fast typing are not rewound); a distinct external
 * replacement, such as "clear", still resets the field.
 */
internal class SyncedTextFieldState(initial: String) : MutableState<TextFieldValue> {
    private val local = mutableStateOf(TextFieldValue(initial, selection = TextRange(initial.length)))
    private var external = initial
    private val awaitingEcho = mutableListOf<String>()

    override var value: TextFieldValue
        get() = local.value
        set(updated) {
            if (updated.text != local.value.text) awaitingEcho += updated.text
            local.value = updated
        }

    fun syncExternal(text: String) {
        if (external == text) return
        external = text
        val acknowledged = awaitingEcho.indexOf(text)
        if (acknowledged >= 0) {
            repeat(acknowledged + 1) { awaitingEcho.removeAt(0) }
            return
        }
        awaitingEcho.clear()
        if (local.value.text != text) {
            local.value = TextFieldValue(text, selection = local.value.selection.clamp(text.length))
        }
    }

    override fun component1(): TextFieldValue = value
    override fun component2(): (TextFieldValue) -> Unit = { value = it }

    private fun TextRange.clamp(length: Int) = TextRange(start.coerceIn(0, length), end.coerceIn(0, length))
}

/**
 * The field value for a text field whose text is owned by a ViewModel that publishes it asynchronously: the cursor and IME
 * composition stay local, and only a real external replacement overwrites them.
 */
@androidx.compose.runtime.Composable
fun rememberCoffeePeekSyncedText(text: String): MutableState<TextFieldValue> {
    val field = androidx.compose.runtime.remember { SyncedTextFieldState(text) }
    androidx.compose.runtime.SideEffect { field.syncExternal(text) }
    return field
}
