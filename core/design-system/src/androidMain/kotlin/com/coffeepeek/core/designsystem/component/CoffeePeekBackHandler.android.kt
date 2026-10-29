package com.coffeepeek.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun CoffeePeekBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)
