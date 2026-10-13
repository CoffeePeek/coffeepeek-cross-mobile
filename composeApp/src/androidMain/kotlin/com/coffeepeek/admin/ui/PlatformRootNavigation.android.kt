package com.coffeepeek.admin.ui

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

@Composable
internal actual fun PlatformRootNavigation(events: Flow<Navigator.NavEvent>, appLink: Navigator.Screen?, consumeLink: () -> Unit, content: @Composable (Navigator.Screen, () -> Unit) -> Unit) = false
