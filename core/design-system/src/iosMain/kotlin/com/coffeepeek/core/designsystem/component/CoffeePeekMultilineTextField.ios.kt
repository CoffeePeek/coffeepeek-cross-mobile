@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSMakeRange
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonSystemItem
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyleBody
import platform.UIKit.UILabel
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextView
import platform.UIKit.UITextViewDelegateProtocol
import platform.UIKit.UIToolbar
import platform.UIKit.accessibilityHint
import platform.UIKit.accessibilityLabel
import platform.UIKit.isAccessibilityElement
import platform.darwin.NSObject

@Composable
internal actual fun CoffeePeekNativeMultilineTextField(
    value: String, onValueChange: (String) -> Unit, label: String, placeholder: String,
    maxLength: Int, modifier: Modifier, enabled: Boolean, errorDescription: String?,
): Boolean {
    val synced = rememberCoffeePeekSyncedText(value)
    val changed by rememberUpdatedState(onValueChange)
    val limit by rememberUpdatedState(maxLength)
    var focused by remember { mutableStateOf(false) }
    val delegate = remember { CoffeePeekMultilineDelegate({ limit }, { text ->
        if (synced.value.text != text) {
            synced.value = TextFieldValue(text)
            changed(text)
        }
    }, { focused = it }) }
    val requester = remember { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, imeBottom) {
        if (focused && imeBottom > 0) requester.bringIntoView()
    }
    val colors = MaterialTheme.colorScheme
    val radius = CoffeePeekDimens.buttonRadius.value.toDouble()
    val lineHeight = UIFont.preferredFontForTextStyle(UIFontTextStyleBody).lineHeight
    val minHeight = maxOf(80.dp, (lineHeight * 2 + 24).dp)
    val maxHeight = maxOf(144.dp, (lineHeight * 4 + 24).dp)
    val text = synced.value.text
    UIKitView(
        factory = { CoffeePeekMultilineInput(delegate) },
        modifier = modifier.heightIn(min = minHeight, max = maxHeight).bringIntoViewRequester(requester),
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true,
        ),
        update = { view ->
            view.configure(text, label, placeholder, enabled, errorDescription)
            view.applyColors(colors, radius, errorDescription != null, enabled)
        },
        onRelease = { it.release() },
    )
    return true
}

private class CoffeePeekMultilineInput(target: CoffeePeekMultilineDelegate) :
    UITextView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), textContainer = null) {
    private val hint = UILabel().apply {
        numberOfLines = 0
        isAccessibilityElement = false
        userInteractionEnabled = false
        adjustsFontForContentSizeCategory = true
    }

    init {
        font = UIFont.preferredFontForTextStyle(UIFontTextStyleBody)
        adjustsFontForContentSizeCategory = true
        autocapitalizationType = UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences
        textContainerInset = UIEdgeInsetsMake(12.0, 14.0, 12.0, 14.0)
        textContainer.lineFragmentPadding = 0.0
        addSubview(hint)
        delegate = target
        inputAccessoryView = coffeePeekMultilineDoneToolbar(this)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        font?.let { hint.font = it }
        val width = (bounds.useContents { size.width } - 28.0).coerceAtLeast(0.0)
        hint.setFrame(CGRectMake(14.0, 12.0, width, font?.lineHeight?.times(2.0) ?: 44.0))
        hint.sizeToFit()
        updateHintVisibility()
    }

    fun configure(value: String, label: String, placeholder: String, enabled: Boolean, error: String?) {
        if (!enabled && isFirstResponder()) resignFirstResponder()
        if (editable != enabled) setEditable(enabled)
        accessibilityLabel = label
        accessibilityHint = error
        hint.text = placeholder
        if (markedTextRange == null) replaceText(value)
        updateHintVisibility()
        setNeedsLayout()
    }

    fun applyColors(colors: ColorScheme, radius: Double, isError: Boolean, enabled: Boolean) {
        backgroundColor = colors.onSurface.copy(alpha = 0.04f).toUIColor()
        textColor = colors.onSurface.copy(alpha = if (enabled) 1f else 0.38f).toUIColor()
        tintColor = colors.primary.toUIColor()
        hint.textColor = colors.onSurfaceVariant.toUIColor()
        layer.cornerRadius = radius
        layer.borderWidth = 1.0
        layer.borderColor = if (isError) colors.error.toUIColor().CGColor else colors.outline.toUIColor().CGColor
        overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
    }

    fun replaceText(value: String) {
        if (text.orEmpty() == value) return
        val (start, length) = selectedRange.useContents { location to length }
        val end = value.length.toULong()
        val location = start.coerceAtMost(end)
        text = value
        selectedRange = NSMakeRange(location, length.coerceAtMost(end - location))
    }

    fun updateHintVisibility() {
        hint.hidden = text.orEmpty().isNotEmpty()
    }

    fun release() {
        delegate = null
        resignFirstResponder()
    }
}

private class CoffeePeekMultilineDelegate(
    private val maxLength: () -> Int,
    private val onChange: (String) -> Unit,
    private val onFocus: (Boolean) -> Unit,
) : NSObject(), UITextViewDelegateProtocol {
    override fun textViewDidBeginEditing(textView: UITextView) = onFocus(true)

    override fun textViewDidEndEditing(textView: UITextView) {
        textViewDidChange(textView)
        onFocus(false)
    }

    override fun textViewDidChange(textView: UITextView) {
        val input = textView as CoffeePeekMultilineInput
        if (input.markedTextRange != null) {
            input.updateHintVisibility()
            return
        }
        val text = input.text.orEmpty()
        val limit = maxLength()
        // A UTF-16 limit must leave pasted emoji intact at the boundary.
        val end = if (text.length > limit && limit > 0 && text[limit - 1].isHighSurrogate() &&
            text[limit].isLowSurrogate()) limit - 1 else limit
        val accepted = text.take(end)
        input.replaceText(accepted)
        input.updateHintVisibility()
        onChange(accepted)
    }
}

private fun coffeePeekMultilineDoneToolbar(input: UITextView) = UIToolbar().apply {
    items = listOf(
        UIBarButtonItem(barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace, target = null, action = null),
        UIBarButtonItem(barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemDone,
            target = input, action = NSSelectorFromString("resignFirstResponder")),
    )
    sizeToFit()
}
