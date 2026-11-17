@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.community

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.analytics.TrackNativeFeatureExposure
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.media.PrefetchPhotos
import com.coffeepeek.core.media.upcomingPhotoUrls
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.community.api.CommunityFeedActions
import com.coffeepeek.feature.community.api.CommunityFeedPresentation
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

private lateinit var feedFactory: (IosCommunityFeed) -> UIViewController
internal fun configureNativeCommunityFeed(factory: (IosCommunityFeed) -> UIViewController) { feedFactory = factory }

internal actual fun nativeCommunityFeedRenderer(): NativeCommunityFeedRenderer? = { data, actions, modifier, header, snackbar ->
    key(data.accountEpoch) { NativeCommunityFeed(data, actions, modifier, header, snackbar) }
}

@Composable
private fun NativeCommunityFeed(data: CommunityFeedPresentation, actions: CommunityFeedActions, modifier: Modifier,
    header: @Composable () -> Unit, snackbar: @Composable () -> Unit) {
    val accounts = koinInject<SessionReader>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val feed = remember { IosCommunityFeed(data, actions, IosCatalogueMedia(fetcher, Constants.BASE_URL), accounts) }
    val controller = remember(feed) { feedFactory(feed) }
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { feed.update(data, actions); controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    DisposableEffect(feed) { onDispose { feed.close() } }
    NativeFeedVisibility(feed, data)
    Box(modifier.fillMaxSize()) {
        NativeEditorControllerHost(controller, Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.TopEnd).padding(horizontal = 16.dp, vertical = 12.dp)) { header() }
        Box(Modifier.align(Alignment.BottomCenter)) { snackbar() }
    }
}

@Composable
private fun NativeFeedVisibility(feed: IosCommunityFeed, data: CommunityFeedPresentation) {
    val fractions by feed.visibleFractions.collectAsState()
    val visible = fractions.filterValues { it >= .1 }.keys
    val interactive = data.interactive && feed.current().active
    TrackNativeFeatureExposure(interactive && !data.loading && "feed" in visible, "feed", "feed")
    data.cards.filter { it.key in visible && it.key != data.teaserKey }.forEach { card ->
        key(card.key) {
            TrackNativeFeatureExposure(interactive, if (card.body.isHome) "feed_home_check_in" else "feed_shop_check_in", "feed")
        }
    }
    if (interactive) PrefetchPhotos(upcomingPhotoUrls(data.cards.map { card ->
        card.key to card.body.photos.take(3).map { it.previewUrl }
    }, visible.toList(), atStart = false))
}
