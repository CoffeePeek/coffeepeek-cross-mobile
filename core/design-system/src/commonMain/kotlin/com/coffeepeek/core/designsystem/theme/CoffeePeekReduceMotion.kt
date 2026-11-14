package com.coffeepeek.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale

/** True when the user (or the system animator scale) disabled motion; callers must then
 * swap animation durations for zero and skip decorative transitions. Shared with features:
 * composeApp's platform check stays for its own native iOS observation. */
@Composable
fun coffeePeekReduceMotion(): Boolean {
    val scale = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor
    return scale == 0f
}

/** Zeroes [durationMillis] when motion is reduced; pass through otherwise. */
@Composable
fun reducedMotionDuration(durationMillis: Int): Int = if (coffeePeekReduceMotion()) 0 else durationMillis
