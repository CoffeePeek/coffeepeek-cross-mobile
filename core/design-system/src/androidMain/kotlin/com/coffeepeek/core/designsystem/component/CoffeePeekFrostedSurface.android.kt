package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.Composable

/** The common surface already consumes the design-system's Android contrast preference. */
@Composable
internal actual fun coffeePeekFrostedSurfacePlatformOpaque(): Boolean = false
