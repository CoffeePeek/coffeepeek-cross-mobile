@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

private fun UIView.textFields(): List<UITextField> =
    if (this is UITextField) listOf(this) else subviews.filterIsInstance<UIView>().filter { !it.hidden && it.alpha > 0 }.flatMap { it.textFields() }

private class FieldDelegate(var changed: (String) -> Unit) : NSObject(), UITextFieldDelegateProtocol {
    var field: UITextField? = null
    var eye: UIButton? = null
    @ObjCAction fun editing(sender: UITextField) { changed(sender.text.orEmpty()) }
    @ObjCAction fun done() { field?.resignFirstResponder() }
    @ObjCAction fun reveal() {
        val input = field ?: return
        val selection = input.selectedTextRange
        val text = input.text
        input.secureTextEntry = !input.secureTextEntry
        // UIKit can clear secure text on its next edit unless it is restored after toggling.
        input.text = text
        input.selectedTextRange = selection
        updateEye()
    }
    fun updateEye() {
        val secure = field?.secureTextEntry == true
        eye?.setImage(UIImage.systemImageNamed(if (secure) "eye" else "eye.slash"), UIControlStateNormal)
        eye?.accessibilityLabel = if (secure) "Показать пароль" else "Скрыть пароль"
    }
    override fun textFieldShouldReturn(textField: UITextField): Boolean {
        if (textField.returnKeyType == UIReturnKeyType.UIReturnKeyNext) {
            val fields = textField.window?.textFields().orEmpty()
            val index = fields.indexOf(textField)
            if (index >= 0 && index + 1 < fields.size) { fields[index + 1].becomeFirstResponder(); return true }
        }
        textField.resignFirstResponder()
        return true
    }
}

@Composable
internal actual fun NativeAuthInput(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, password: Boolean, newPassword: Boolean, next: Boolean, error: Boolean, keyboardType: KeyboardType, modifier: Modifier): Boolean {
    val callback = rememberUpdatedState(onValueChange)
    val delegate = remember { FieldDelegate { callback.value(it) } }
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    UIKitView(factory = {
        UITextField().apply {
            delegate.field = this
            this.delegate = delegate
            borderStyle = UITextBorderStyle.UITextBorderStyleRoundedRect
            font = UIFont.preferredFontForTextStyle(UIFontTextStyleBody)
            adjustsFontForContentSizeCategory = true
            secureTextEntry = password
            autocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeNo
            autocapitalizationType = UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
            spellCheckingType = UITextSpellCheckingType.UITextSpellCheckingTypeNo
            textContentType = when {
                newPassword -> UITextContentTypeNewPassword
                password -> UITextContentTypePassword
                keyboardType == KeyboardType.Email -> UITextContentTypeUsername
                keyboardType == KeyboardType.Text -> UITextContentTypeName
                else -> null
            }
            this.keyboardType = when (keyboardType) {
                KeyboardType.Email -> UIKeyboardTypeEmailAddress
                KeyboardType.Uri -> UIKeyboardTypeURL
                KeyboardType.Number, KeyboardType.NumberPassword -> UIKeyboardTypeNumberPad
                KeyboardType.Phone -> UIKeyboardTypePhonePad
                else -> UIKeyboardTypeDefault
            }
            addTarget(delegate, NSSelectorFromString("editing:"), UIControlEventEditingChanged)
            val toolbar = UIToolbar(frame = CGRectMake(0.0, 0.0, 320.0, 44.0))
            toolbar.items = listOf(
                UIBarButtonItem(barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace, target = null, action = null),
                UIBarButtonItem(title = "Готово", style = UIBarButtonItemStyle.UIBarButtonItemStyleDone, target = delegate, action = NSSelectorFromString("done")),
            )
            inputAccessoryView = toolbar
            if (password) {
                val button = UIButton(frame = CGRectMake(0.0, 0.0, 44.0, 44.0))
                button.addTarget(delegate, NSSelectorFromString("reveal"), UIControlEventTouchUpInside)
                delegate.eye = button
                rightView = button
                rightViewMode = UITextFieldViewMode.UITextFieldViewModeAlways
                delegate.updateEye()
            }
        }
    }, modifier = modifier, properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, interactionMode = UIKitInteropInteractionMode.NonCooperative), update = {
        if (it.text != value) it.text = value
        it.placeholder = placeholder
        it.accessibilityLabel = label
        it.accessibilityHint = if (error) "Проверьте введённое значение" else null
        it.returnKeyType = if (next) UIReturnKeyType.UIReturnKeyNext else UIReturnKeyType.UIReturnKeyDone
        it.overrideUserInterfaceStyle = if (dark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
        it.layer.borderWidth = if (error) 1.0 else 0.0
        it.layer.borderColor = UIColor.systemRedColor.CGColor
        it.layer.cornerRadius = 10.0
    }, onRelease = {
        it.delegate = null
        it.removeTarget(delegate, NSSelectorFromString("editing:"), UIControlEventEditingChanged)
        delegate.field = null
        delegate.eye = null
    })
    return true
}

@Composable
internal actual fun NativeTimePickerRow(label: String, value: String, onAdjust: (Int) -> Unit): Boolean {
    var shown by remember { mutableStateOf(false) }
    val parts = value.split(':')
    val minutes = (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = { shown = true }) { Text(value) }
    }
    if (shown) NativeDatePicker(label, minutes * 60_000L, null, true, { millis ->
        val selected = ((millis / 60_000) % (24 * 60)).toInt()
        onAdjust(selected - minutes)
    }, { shown = false })
    return true
}

private class DoneTarget(val action: () -> Unit) : NSObject() {
    @ObjCAction fun done() = action()
}

/** Numeric Compose fields have no return key. Use a UIKit toolbar tied to the keyboard layout guide. */
@Composable
internal actual fun NativeKeyboardDoneAccessory(visible: Boolean, onDone: () -> Unit) {
    if (!visible) return
    val controller = LocalUIViewController.current
    val action = rememberUpdatedState(onDone)
    DisposableEffect(controller) {
        val host = controller.parentViewController?.view ?: controller.view
        val target = DoneTarget { host.endEditing(true); action.value() }
        val toolbar = UIToolbar().apply {
            translatesAutoresizingMaskIntoConstraints = false
            items = listOf(
                UIBarButtonItem(barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace, target = null, action = null),
                UIBarButtonItem(title = "Готово", style = UIBarButtonItemStyle.UIBarButtonItemStyleDone, target = target, action = NSSelectorFromString("done")),
            )
        }
        host.addSubview(toolbar)
        val constraints = listOf(
            toolbar.leadingAnchor.constraintEqualToAnchor(host.safeAreaLayoutGuide.leadingAnchor),
            toolbar.trailingAnchor.constraintEqualToAnchor(host.safeAreaLayoutGuide.trailingAnchor),
            toolbar.bottomAnchor.constraintEqualToAnchor(host.keyboardLayoutGuide.topAnchor),
            toolbar.heightAnchor.constraintEqualToConstant(44.0),
        )
        NSLayoutConstraint.activateConstraints(constraints)
        onDispose {
            NSLayoutConstraint.deactivateConstraints(constraints)
            toolbar.removeFromSuperview()
            toolbar.items = null
            target.action // Retain the UIBarButtonItem's weak target until disposal.
        }
    }
}
