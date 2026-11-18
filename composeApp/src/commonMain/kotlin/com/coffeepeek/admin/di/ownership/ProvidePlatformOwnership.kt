package com.coffeepeek.admin.di.ownership

import androidx.compose.runtime.Composable

@Composable
internal expect fun ProvidePlatformOwnership(content: @Composable () -> Unit)
