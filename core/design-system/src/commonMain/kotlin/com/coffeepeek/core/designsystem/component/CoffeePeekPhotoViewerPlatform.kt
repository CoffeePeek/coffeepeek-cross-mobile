package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Root supplies exposure occlusion and the existing platform reduced-motion setting. */
class CoffeePeekPhotoViewerPlatform(
    val overlay: @Composable () -> Unit = {},
    val reduceMotion: @Composable () -> Boolean = { false },
)

val LocalCoffeePeekPhotoViewerPlatform = staticCompositionLocalOf { CoffeePeekPhotoViewerPlatform() }
