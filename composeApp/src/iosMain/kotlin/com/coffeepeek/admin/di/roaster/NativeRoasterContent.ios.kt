@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.roaster

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.analytics.Entity
import com.coffeepeek.admin.analytics.TrackEntityDetail
import com.coffeepeek.admin.analytics.TrackNativeFeatureExposure
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.admin.ui.component.NativeNavigationBar
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.roaster.impl.*
import com.coffeepeek.feature.settings.api.LanguagePort
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

private lateinit var roasterController: (IosRoasterScreen) -> UIViewController
internal fun configureNativeRoasters(factory: (IosRoasterScreen) -> UIViewController) { roasterController = factory }

internal actual fun nativeRoasterDiscoveryContent(): NativeRoasterDiscoveryContent? = { session, host ->
    RoasterDiscoveryNativeContent(session, host)
    true
}

internal actual fun nativeRoasterDetailContent(): NativeRoasterDetailContent? = { session, host ->
    RoasterDetailNativeContent(session, host)
    true
}

@Composable
private fun RoasterDiscoveryNativeContent(session: NativeRoasterListSession, host: NativeRoasterDiscoveryHost) {
    val language = koinInject<LanguagePort>()
    val accounts = koinInject<SessionReader>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val generation = accounts.accountGeneration()
    val source = remember(session, generation) { IosRoasterDiscovery(session, host, language, accounts, IosCatalogueMedia(fetcher, Constants.BASE_URL)) }
    val controller = remember(source) { roasterController(source) }
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { source.update(host); controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    LaunchedEffect(session, host.query, host.selectedRoasters, host.favoritesOnly) {
        session.criteria(host.query, host.selectedRoasters, host.favoritesOnly)
    }
    DisposableEffect(source) { onDispose { source.close() } }
    RoasterNativeExposures(source, if (host.search) "catalog" else "discovery")
    NativeEditorControllerHost(controller, host.modifier.fillMaxSize())
}

@Composable
private fun RoasterDetailNativeContent(session: NativeRoasterDetailSession, host: NativeRoasterDetailHost) {
    val language = koinInject<LanguagePort>()
    val accounts = koinInject<SessionReader>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val ownership = koinInject<BusinessOwnershipEntry>()
    val generation = accounts.accountGeneration()
    val source = remember(session, generation) {
        IosRoasterDetail(session, host, language, accounts, IosCatalogueMedia(fetcher, Constants.BASE_URL)) {
            roasterOwnershipController(session, ownership)
        }
    }
    val controller = remember(source) { roasterController(source) }
    val theme by ThemeManager.themeMode.collectAsState()
    val snapshot by session.state.collectAsState(session.current())
    val changed = ownership.ProfileChanges()
    LaunchedEffect(changed) { if (changed > 0) session.retry() }
    SideEffect { source.update(host); controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    DisposableEffect(source) { onDispose { source.close() } }
    TrackEntityDetail(Entity.Roaster, snapshot.details != null && source.isCurrent())
    RoasterNativeExposures(source, "roaster_detail")
    NativeNavigationBar("", source::back, customBack = true, action = null)
    NativeEditorControllerHost(controller, host.modifier.fillMaxSize())
}

private fun roasterOwnershipController(session: NativeRoasterDetailSession, ownership: BusinessOwnershipEntry) = ComposeUIViewController {
    AppContent {
        val state by session.state.collectAsState(session.current())
        state.details?.let { details ->
            ownership.ProfileActions("Roaster", details.id, details.address.slug, {}, {}, {}, {})
        }
    }
}

@Composable
private fun RoasterNativeExposures(source: IosRoasterScreen, placement: String) {
    val fractions by source.fractions.collectAsState()
    val sections = fractions.filterValues { it >= .1 }.keys
    if (placement == "roaster_detail") {
        listOf("photos", "contacts", "shops").forEach { section ->
            key(section) { TrackNativeFeatureExposure(source.isCurrent() && section in sections, "roaster_$section", placement) }
        }
    } else {
        TrackNativeFeatureExposure(source.isCurrent() && "catalog" in sections, "roaster_catalog", placement)
        if (placement == "catalog") TrackNativeFeatureExposure(source.isCurrent() && "search" in sections, "search", "roasters")
    }
    TrackNativeFeatureExposure(source.isCurrent() && "favorite" in sections, "favorites",
        if (placement == "roaster_detail") "roaster_card" else "catalog_card")
}
