package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.admin.utils.currentEpochMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Shared app date-label policy for public Shop cards and personal history. */
@Composable
internal fun rememberCheckInDateLabel(): (String) -> String {
    val labels = reviewDateLabels()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(currentEpochMillis(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                value = currentEpochMillis()
                delay(60_000)
            }
        }
    }
    return { formatReviewDisplayDate(it, labels, now) }
}
