package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.runtime.Composable

@Composable
internal expect fun PlatformHistorySessionUnavailable(onRetry: () -> Unit, onBack: () -> Unit): Boolean
