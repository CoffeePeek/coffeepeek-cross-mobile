package com.coffeepeek.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** Optional host/preview override; null follows the platform. Does not change the palette. */
val LocalCoffeePeekOpaqueGlassOverride = compositionLocalOf<Boolean?> { null }

internal val LocalCoffeePeekOpaqueGlass = compositionLocalOf { false }

/** Native iOS materials own their accessibility; custom Android glass uses OS contrast. */
@Composable
internal expect fun coffeePeekPlatformOpaqueGlass(): Boolean
