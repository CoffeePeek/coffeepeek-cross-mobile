package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformHistorySessionUnavailable(onRetry: () -> Unit, onBack: () -> Unit) = false
