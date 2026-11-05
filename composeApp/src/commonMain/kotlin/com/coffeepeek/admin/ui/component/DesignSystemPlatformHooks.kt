package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable

/** Installs the platform's native presenters for the shared design-system components around [content]. */
@Composable
internal expect fun ProvideDesignSystemPlatformHooks(content: @Composable () -> Unit)
