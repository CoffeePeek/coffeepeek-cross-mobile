@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.UIControlEventEditingDidBegin
import platform.UIKit.UIControlEventEditingDidEnd
import platform.UIKit.UIControlEventEditingDidEndOnExit
import platform.UIKit.UITextField
import platform.darwin.NSObject

@Composable
internal actual fun CoffeePeekPlatformSearchField(
    value: String, onValueChange: (String) -> Unit, placeholder: String, clearDescription: String,
    enabled: Boolean, readOnly: Boolean, modifier: Modifier, keyboardOptions: KeyboardOptions,
    onSearch: () -> Unit, onFocusChanged: (Boolean) -> Unit,
): Boolean {
    if (readOnly || keyboardOptions.imeAction != ImeAction.Search ||
        keyboardOptions.keyboardType !in listOf(KeyboardType.Text, KeyboardType.Unspecified)) return false
    val changed = rememberUpdatedState(onValueChange)
    val search = rememberUpdatedState(onSearch)
    val focusChanged = rememberUpdatedState(onFocusChanged)
    val focus = remember { FocusRequester() }
    val manager = LocalFocusManager.current
    val field = remember { SyncedTextFieldState(value) }
    SideEffect { field.syncExternal(value) }
    val target = remember { CoffeePeekSearchTarget({ text ->
        if (field.value.text != text) {
            field.value = TextFieldValue(text)
            changed.value(text)
        }
    }, { search.value() }) {
        if (it) focus.requestFocus() else manager.clearFocus()
        focusChanged.value(it)
    } }
    val colors = MaterialTheme.colorScheme
    val displayedText = field.value.text
    CoffeePeekNativeGlassView(
        factory = { CoffeePeekSearchInput(target) },
        modifier = modifier.focusRequester(focus).onFocusChanged {
            if (!it.hasFocus) target.field?.resignFirstResponder()
        }.focusable(enabled),
        update = { view ->
            view.configure(displayedText, placeholder, clearDescription, enabled, keyboardOptions)
            view.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
            view.field.textColor = colors.onSurface.toUIColor()
            view.field.tintColor = colors.coffeePeekAccentForeground.toUIColor()
            view.setIconTint(colors.onSurfaceVariant.toUIColor())
        },
        onRelease = { it.release() },
    )
    return true
}

internal class CoffeePeekSearchTarget(
    val onChanged: (String) -> Unit,
    val onSearch: () -> Unit,
    val onFocusChanged: (Boolean) -> Unit,
) : NSObject() {
    var field: UITextField? = null
    private var replacingText = false

    @ObjCAction fun changed(sender: UITextField) {
        if (!replacingText) onChanged(sender.text.orEmpty())
    }
    @ObjCAction fun focused(sender: UITextField) = onFocusChanged(true)
    @ObjCAction fun unfocused(sender: UITextField) = onFocusChanged(false)
    @ObjCAction fun submitted(sender: UITextField) {
        sender.resignFirstResponder()
        onSearch()
    }

    fun attach(input: UITextField) {
        field = input
        input.addTarget(this, NSSelectorFromString("changed:"), UIControlEventEditingChanged)
        input.addTarget(this, NSSelectorFromString("focused:"), UIControlEventEditingDidBegin)
        input.addTarget(this, NSSelectorFromString("unfocused:"), UIControlEventEditingDidEnd)
        input.addTarget(this, NSSelectorFromString("submitted:"), UIControlEventEditingDidEndOnExit)
    }

    fun detach() {
        field?.removeTarget(this, null, UIControlEventEditingChanged or UIControlEventEditingDidBegin or
            UIControlEventEditingDidEnd or UIControlEventEditingDidEndOnExit)
        field = null
    }

    fun clear() {
        replaceText {
            field?.unmarkText()
            field?.text = ""
        }
        onChanged("")
    }

    fun replaceText(update: () -> Unit) {
        replacingText = true
        try { update() } finally { replacingText = false }
    }
}
