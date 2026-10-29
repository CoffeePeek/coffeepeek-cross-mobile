package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

@Composable
internal fun Modifier.mapInteraction(onInteraction: () -> Unit): Modifier {
    val currentInteraction by rememberUpdatedState(onInteraction)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            // Let native gestures and button clicks finish before presenting a system dialog.
            do {
                val event = awaitPointerEvent(PointerEventPass.Final)
            } while (event.changes.any { it.pressed })
            currentInteraction()
        }
    }
}
