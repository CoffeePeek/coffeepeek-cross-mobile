package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// UIKit's overlaid numeric-keyboard toolbar is not included in WindowInsets.ime.
internal val LocalKeyboardAccessorySpace = staticCompositionLocalOf<SnapshotStateMap<Any, Dp>?> { null }

/** Keeps the header, scrolling fields and footer in the same keyboard-safe viewport. */
@Composable
internal fun KeyboardAwareForm(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val accessories = remember { mutableStateMapOf<Any, Dp>() }
    CompositionLocalProvider(LocalKeyboardAccessorySpace provides accessories) {
        Box(
            // Apply keyboard space before a caller's height cap. Children consume the
            // same IME inset, so a scrollable field list does not add it a second time.
            Modifier.imePadding().padding(bottom = accessories.values.maxOrNull() ?: 0.dp).then(modifier),
        ) {
            content()
        }
    }
}
