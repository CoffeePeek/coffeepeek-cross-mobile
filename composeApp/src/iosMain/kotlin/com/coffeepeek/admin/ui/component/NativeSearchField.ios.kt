@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.*
import platform.darwin.NSObject

private class SearchDelegate : NSObject(), UISearchBarDelegateProtocol {
    var changed: (String) -> Unit = {}
    var focused: (Boolean) -> Unit = {}
    var submitted: () -> Unit = {}
    var cancelled: (() -> Unit)? = null

    override fun searchBar(searchBar: UISearchBar, textDidChange: String) {
        if (searchBar.searchTextField.markedTextRange == null) changed(textDidChange)
    }
    override fun searchBarTextDidBeginEditing(searchBar: UISearchBar) {
        searchBar.setShowsCancelButton(true, !UIAccessibilityIsReduceMotionEnabled())
        focused(true)
    }
    override fun searchBarTextDidEndEditing(searchBar: UISearchBar) {
        searchBar.setShowsCancelButton(false, !UIAccessibilityIsReduceMotionEnabled())
        focused(false)
    }
    override fun searchBarSearchButtonClicked(searchBar: UISearchBar) {
        searchBar.resignFirstResponder()
        submitted()
    }
    override fun searchBarCancelButtonClicked(searchBar: UISearchBar) {
        searchBar.text = ""
        changed("")
        searchBar.resignFirstResponder()
        cancelled?.invoke()
    }
}

@Composable
internal actual fun NativeSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, onSearch: () -> Unit, onFocusChanged: (Boolean) -> Unit, onCancel: (() -> Unit)?, modifier: Modifier): Boolean {
    val changed = rememberUpdatedState(onValueChange)
    val focused = rememberUpdatedState(onFocusChanged)
    val submitted = rememberUpdatedState(onSearch)
    val cancelled = rememberUpdatedState(onCancel)
    val delegate = remember { SearchDelegate().apply {
        this.changed = { changed.value(it) }
        this.focused = { focused.value(it) }
        this.submitted = { submitted.value() }
        this.cancelled = { cancelled.value?.invoke() }
    } }
    val colors = MaterialTheme.colorScheme
    UIKitView(factory = {
        UISearchBar().apply {
            searchBarStyle = UISearchBarStyle.UISearchBarStyleMinimal
            returnKeyType = UIReturnKeyType.UIReturnKeySearch
            autocapitalizationType = UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
            autocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeNo
            searchTextField.font = UIFont.preferredFontForTextStyle(UIFontTextStyleBody)
            searchTextField.adjustsFontForContentSizeCategory = true
            this.delegate = delegate
        }
    }, modifier = modifier,
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true, interactionMode = UIKitInteropInteractionMode.NonCooperative),
        update = { bar ->
            if (bar.searchTextField.markedTextRange == null && bar.text != value) bar.text = value
            bar.placeholder = placeholder
            bar.searchTextField.accessibilityLabel = placeholder
            bar.tintColor = UIColor.colorWithRed(colors.primary.red.toDouble(), colors.primary.green.toDouble(), colors.primary.blue.toDouble(), 1.0)
            bar.overrideUserInterfaceStyle = if (colors.background.luminance() < .5f) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
        },
        onRelease = { it.delegate = null; it.resignFirstResponder() },
    )
    return true
}
