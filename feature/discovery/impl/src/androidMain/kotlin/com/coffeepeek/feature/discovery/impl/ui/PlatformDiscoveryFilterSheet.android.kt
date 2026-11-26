package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState

@Composable
internal actual fun PlatformDiscoveryFilterSheet(state: SearchState, onAction: (SearchAction) -> Unit): Boolean = false
