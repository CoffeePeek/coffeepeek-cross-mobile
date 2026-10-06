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
    val field = remember {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }
    val external = remember { LastExternalValue(value) }
    SideEffect {
        if (external.value != value) {
            external.value = value
            if (field.value.text != value) {
                val selection = field.value.selection
                field.value = TextFieldValue(
                    text = value,
                    selection = TextRange(
                        selection.start.coerceIn(0, value.length),
                        selection.end.coerceIn(0, value.length),
                    ),
                )
            }
        }
    }
    return field
}

private class LastExternalValue(var value: String)

/** Apply a field limit before updating local cursor state and publishing text. */
fun TextFieldValue.limitTextLength(maxLength: Int): TextFieldValue {
    if (text.length <= maxLength) return this
    return TextFieldValue(
        text = text.take(maxLength),
        selection = TextRange(
            selection.start.coerceIn(0, maxLength),
            selection.end.coerceIn(0, maxLength),
        ),
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
    )
}
