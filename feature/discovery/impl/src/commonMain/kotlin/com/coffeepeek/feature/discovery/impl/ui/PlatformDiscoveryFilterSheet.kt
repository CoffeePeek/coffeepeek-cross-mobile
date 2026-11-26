package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.discovery.impl.search.SearchAction
import com.coffeepeek.feature.discovery.impl.search.SearchState

/** iOS presents this sheet in SwiftUI. Other hosts keep the existing Compose presentation. */
@Composable
internal expect fun PlatformDiscoveryFilterSheet(state: SearchState, onAction: (SearchAction) -> Unit): Boolean
