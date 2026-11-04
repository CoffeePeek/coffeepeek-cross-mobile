@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIAction
import platform.UIKit.UIButton
import platform.UIKit.UIButtonConfiguration
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyleBody
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextBorderStyle
import platform.UIKit.UITextField
import platform.UIKit.UIView
import platform.UIKit.accessibilityLabel

/** Glass and input are siblings in one UIKit overlay; no underlay hole can expose another screen. */
internal class CoffeePeekSearchInput(private val target: CoffeePeekSearchTarget) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val field = UITextField()
    private val glass = coffeePeekGlassMaterial()
    private val searchIcon = UIImageView(UIImage.systemImageNamed("magnifyingglass"))
    private val clear = coffeePeekSearchClearButton(target)

    init {
        backgroundColor = UIColor.clearColor
        addSubview(glass)
        glass.contentView.addSubview(searchIcon)
        glass.contentView.addSubview(field)
        glass.contentView.addSubview(clear)
        field.backgroundColor = UIColor.clearColor
        field.borderStyle = UITextBorderStyle.UITextBorderStyleNone
        field.font = UIFont.preferredFontForTextStyle(UIFontTextStyleBody)
        field.adjustsFontForContentSizeCategory = true
        clear.configuration = UIButtonConfiguration.plainButtonConfiguration().apply { image = UIImage.systemImageNamed("xmark.circle.fill") }
        target.attach(field)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val width = bounds.useContents { size.width }
        val height = bounds.useContents { size.height }
        glass.setFrame(CGRectMake(0.0, 2.0, width, (height - 4.0).coerceAtLeast(0.0)))
        glass.updateCoffeePeekGlassCorners()
        searchIcon.setFrame(CGRectMake(12.0, (height - 24.0) / 2.0, 20.0, 20.0))
        clear.setFrame(CGRectMake((width - 44.0).coerceAtLeast(0.0), 0.0, 44.0, height - 4.0))
        val trailing = if (clear.hidden) 12.0 else 44.0
        field.setFrame(CGRectMake(40.0, 0.0, (width - 40.0 - trailing).coerceAtLeast(0.0), height - 4.0))
    }

    fun configure(value: String, placeholder: String, clearDescription: String, enabled: Boolean, options: KeyboardOptions) {
        updateText(value)
        field.placeholder = placeholder
        field.accessibilityLabel = placeholder
        field.enabled = enabled
        clear.enabled = enabled
        clear.hidden = value.isEmpty()
        clear.accessibilityLabel = clearDescription
        field.returnKeyType = UIReturnKeyType.UIReturnKeySearch
        field.autocorrectionType = if (options.autoCorrectEnabled == false) UITextAutocorrectionType.UITextAutocorrectionTypeNo
            else UITextAutocorrectionType.UITextAutocorrectionTypeDefault
        field.autocapitalizationType = options.capitalization.nativeCapitalization()
        setNeedsLayout()
    }

    fun setIconTint(color: UIColor) {
        searchIcon.tintColor = color
        clear.tintColor = color
    }

    fun release() {
        field.resignFirstResponder()
        target.detach()
    }

    private fun updateText(value: String) {
        if (field.text.orEmpty() == value) return
        val selection = field.selectedTextRange
        val beginning = field.beginningOfDocument
        val start = selection?.let { field.offsetFromPosition(beginning, it.start) } ?: value.length.toLong()
        val end = selection?.let { field.offsetFromPosition(beginning, it.end) } ?: start
        // Local edits/acknowledged echoes already match. A different accepted model value is
        // authoritative: end marked text without letting its commit restore the previous query.
        target.replaceText {
            field.unmarkText()
            field.text = value
            val first = field.positionFromPosition(beginning, start.coerceIn(0, value.length.toLong()))
            val last = field.positionFromPosition(beginning, end.coerceIn(0, value.length.toLong()))
            if (first != null && last != null) field.selectedTextRange = field.textRangeFromPosition(first, last)
        }
    }
}

/** Capture the target parameter directly; capturing the input's property would retain its UIView. */
private fun coffeePeekSearchClearButton(target: CoffeePeekSearchTarget): UIButton =
    UIButton.buttonWithConfiguration(UIButtonConfiguration.plainButtonConfiguration(),
        UIAction.actionWithHandler { target.clear() })

private fun KeyboardCapitalization.nativeCapitalization(): UITextAutocapitalizationType = when (this) {
    KeyboardCapitalization.Characters -> UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters
    KeyboardCapitalization.Words -> UITextAutocapitalizationType.UITextAutocapitalizationTypeWords
    KeyboardCapitalization.Sentences -> UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences
    else -> UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
}
