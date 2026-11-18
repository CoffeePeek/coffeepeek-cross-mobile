package com.coffeepeek.feature.discovery.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.coffeepeek.feature.discovery.api.*
import com.coffeepeek.feature.discovery.impl.map.MapViewModel
import com.coffeepeek.feature.discovery.impl.search.SearchViewModel

@Composable
internal expect fun PlatformDiscoveryContent(
    search: SearchViewModel, map: MapViewModel, host: DiscoveryHost,
    roasters: DiscoveryRoasterSlots?, analytics: DiscoveryAnalytics, callbacks: TabCallbacks,
    bottomClearance: Dp, focus: MapFocus?, modifier: Modifier, picker: Boolean,
): Boolean
