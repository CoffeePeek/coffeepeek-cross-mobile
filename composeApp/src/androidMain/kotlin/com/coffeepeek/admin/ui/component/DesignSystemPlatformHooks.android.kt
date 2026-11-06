package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable

/** Android renders the design-system components itself. */
@Composable
internal actual fun ProvideDesignSystemPlatformHooks(content: @Composable () -> Unit) = content()
