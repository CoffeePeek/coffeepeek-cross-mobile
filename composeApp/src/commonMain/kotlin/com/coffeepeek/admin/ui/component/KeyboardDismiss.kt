package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** Background taps dismiss editing; iOS also observes vertical drags through UIKit. */
internal fun Modifier.dismissKeyboardOnBackgroundGestures(): Modifier = composed {
    PlatformKeyboardDismissOnDrag()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    pointerInput(focusManager, keyboard) {
        detectTapGestures(onTap = {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        })
    }
}

@Composable
internal expect fun PlatformKeyboardDismissOnDrag()
