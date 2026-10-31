package com.coffeepeek.admin.sandbox.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer

/** Manual fixture uses Compose's own scale, without writing system settings. */
internal fun ComponentActivity.sandboxMotionContext(): CompositionContext? {
    if (!intent.getBooleanExtra("sandbox_reduce_motion", false)) return null
    val scale = object : MotionDurationScale { override val scaleFactor = 0f }
    return window.decorView.createLifecycleAwareWindowRecomposer(scale, lifecycle)
}
