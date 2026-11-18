@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.discovery

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.coffeepeek.admin.analytics.TrackNativeFeatureExposure
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.discovery.impl.ui.DiscoveryIosPresentation
import com.coffeepeek.feature.discovery.impl.ui.configureIosDiscoveryContent
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal fun configureNativeDiscovery(factory: (IosDiscovery) -> UIViewController) {
    configureIosDiscoveryContent { data, modifier -> NativeDiscovery(data, modifier, factory) }
}

@Composable
private fun NativeDiscovery(data: DiscoveryIosPresentation, modifier: Modifier, factory: (IosDiscovery) -> UIViewController) {
    val accounts = koinInject<SessionReader>()
    val epoch by remember(accounts) { accounts.observeSession().map { accounts.discoveryAccount() } }
        .collectAsState(initial = accounts.discoveryAccount())
    key(epoch) { NativeDiscoverySession(data, modifier, factory, accounts) }
}

@Composable
private fun NativeDiscoverySession(data: DiscoveryIosPresentation, modifier: Modifier,
    factory: (IosDiscovery) -> UIViewController, accounts: SessionReader) {
    val fetcher = koinInject<ImageUrlFetcher>()
    val locals = currentCompositionLocalContext
    val discovery = remember { IosDiscovery(data, IosCatalogueMedia(fetcher, Constants.BASE_URL), accounts, locals) }
    val controller = remember(discovery) { factory(discovery) }
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { discovery.update(data, locals); controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    DisposableEffect(discovery) { onDispose { discovery.close() } }
    NativeDiscoveryExposures(discovery, data)
    NativeEditorControllerHost(controller, modifier.fillMaxSize())
}

@Composable
private fun NativeDiscoveryExposures(discovery: IosDiscovery, data: DiscoveryIosPresentation) {
    val visible by discovery.visibleElements.collectAsState()
    val active = data.state.active && !data.state.roastersExpanded
    TrackNativeFeatureExposure(active && "search" in visible, "search", "shops")
    TrackNativeFeatureExposure(active && "filters" in visible, "filters", "shops")
    TrackNativeFeatureExposure(active && "shops" in visible, "shop_catalog", "discovery")
    TrackNativeFeatureExposure(active && "favorite" in visible, "favorites", "catalog_card")
    TrackNativeFeatureExposure(active && "map" in visible, "map", if (data.state.mapExpanded) "discovery" else "home")
}
