package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable

@Composable
internal actual fun ProvidePlatformCheckInEditors(content: @Composable () -> Unit) = content()
