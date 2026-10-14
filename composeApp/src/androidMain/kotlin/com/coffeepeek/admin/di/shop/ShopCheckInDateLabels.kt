package com.coffeepeek.admin.di.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.admin.ui.component.formatReviewDisplayDate
import com.coffeepeek.admin.utils.currentEpochMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Reuse app date labels and their minute refresh without putting platform time into feature UI. */
@Composable
internal fun rememberShopCheckInDateLabel(): (String) -> String {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now by produceState(currentEpochMillis(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                value = currentEpochMillis()
                delay(60_000)
            }
        }
    }
    return { formatReviewDisplayDate(it, now) }
}
