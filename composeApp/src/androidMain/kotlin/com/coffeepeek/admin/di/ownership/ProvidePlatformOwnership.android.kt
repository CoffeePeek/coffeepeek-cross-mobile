package com.coffeepeek.admin.di.ownership

import androidx.compose.runtime.Composable

@Composable
internal actual fun ProvidePlatformOwnership(content: @Composable () -> Unit) = content()
