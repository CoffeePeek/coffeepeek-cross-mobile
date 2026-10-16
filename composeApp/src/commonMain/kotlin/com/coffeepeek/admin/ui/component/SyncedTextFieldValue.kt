package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Keeps the cursor local while a ViewModel publishes the edited string asynchronously. */
@Composable
fun rememberSyncedTextFieldValue(value: String): MutableState<TextFieldValue> {
    val field = remember { SyncedTextFieldState(value) }
    SideEffect { field.syncExternal(value) }
    return field
}

/** Local edits win over delayed echoes; a distinct external replacement still resets the field. */
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
            return // Preserve the newer text, selection and IME composition, including autocorrect.
        }
        awaitingEcho.clear()
        if (local.value.text != text) {
            local.value = TextFieldValue(text, selection = local.value.selection.clampTo(text.length))
        }
    }

    override fun component1(): TextFieldValue = value
    override fun component2(): (TextFieldValue) -> Unit = { value = it }
}

private fun TextRange.clampTo(length: Int) = TextRange(start.coerceIn(0, length), end.coerceIn(0, length))

/** Cursor/composition updates stay local and do not re-run the entire form's change handlers. */
internal fun MutableState<TextFieldValue>.updateFromInput(
    updated: TextFieldValue,
    onTextChange: (String) -> Unit,
    acceptText: ((String) -> Boolean)? = null,
) {
    if (acceptText?.invoke(updated.text) == false) return
    val textChanged = value.text != updated.text
    value = updated
    if (textChanged) onTextChange(updated.text)
}

/** Apply a field limit before updating local cursor state and publishing text. */
fun TextFieldValue.limitTextLength(maxLength: Int): TextFieldValue {
    require(maxLength >= 0)
    if (text.length <= maxLength) return this
    // Do not leave half an emoji/supplementary character at the limit.
    val end = if (maxLength > 0 && text[maxLength - 1].isHighSurrogate() && text[maxLength].isLowSurrogate()) maxLength - 1 else maxLength
    return TextFieldValue(
        text = text.take(end),
        selection = selection.clampTo(end),
        composition = composition?.clampTo(end)?.takeUnless { it.collapsed },
    )
}

fun TextFieldValue.filterText(predicate: (Char) -> Boolean): TextFieldValue {
    val filtered = text.filter(predicate)
    if (filtered == text) return this
    return TextFieldValue(
        text = filtered,
        selection = TextRange(
            text.take(selection.start).count(predicate),
            text.take(selection.end).count(predicate),
        ),
        composition = composition?.let {
            TextRange(text.take(it.start).count(predicate), text.take(it.end).count(predicate))
        }?.takeUnless { it.collapsed },
    )
}
