package com.coffeepeek.core.designsystem.theme

import android.app.UiModeManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

internal actual val coffeePeekUsesNativeGlass: Boolean = false

@Composable
internal actual fun coffeePeekPlatformOpaqueGlass(): Boolean {
    if (Build.VERSION.SDK_INT < 34) return false
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(UiModeManager::class.java) }
    var contrast by remember(manager) { mutableFloatStateOf(manager?.contrast ?: 0f) }
    DisposableEffect(manager, context) {
        val listener = UiModeManager.ContrastChangeListener { contrast = it }
        manager?.addContrastChangeListener(context.mainExecutor, listener)
        contrast = manager?.contrast ?: 0f
        onDispose { manager?.removeContrastChangeListener(listener) }
    }
    return contrast > 0f
}
