package com.coffeepeek.core.designsystem.theme

import androidx.compose.runtime.Composable

internal actual val coffeePeekUsesNativeGlass: Boolean = true

/** UIKit materials handle Increase Contrast and Reduce Transparency themselves. */
@Composable
internal actual fun coffeePeekPlatformOpaqueGlass(): Boolean = false
