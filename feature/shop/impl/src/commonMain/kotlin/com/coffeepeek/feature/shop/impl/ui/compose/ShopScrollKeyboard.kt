package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.ui.Modifier

/** Dragging the form scroll closes the keyboard, the standard Android behavior. */
internal expect fun Modifier.closeKeyboardOnDrag(): Modifier
