@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.community

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.nativeScreenHost
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.community.api.*
import com.coffeepeek.feature.homecheckin.api.HomeCheckInNativeDetailRenderer
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

private lateinit var detailFactory: (IosCheckInDetail) -> UIViewController
internal fun configureNativeCheckInDetails(factory: (IosCheckInDetail) -> UIViewController) { detailFactory = factory }

@Composable
internal actual fun nativeCheckInDetailAvailable(): Boolean = LocalUIViewController.current.nativeScreenHost()?.supportsNativeDetailBar == true

internal actual fun nativeCommunityDetailRenderer(): NativeCommunityDetailRenderer? = { data, actions, modifier, overlay ->
    NativeCheckInDetail(NativeCheckInDetailPresentation(data.card, data.title, data.backLabel,
        loadingLabel = data.loadingLabel, errorText = listOfNotNull(data.errorTitle, data.errorText).joinToString("\n").takeIf(String::isNotBlank),
        retryLabel = data.retryLabel), actions, modifier, overlay)
}

internal actual fun nativeHomeDetailRenderer(): HomeCheckInNativeDetailRenderer? = { data, actions, modifier ->
    NativeCheckInDetail(data.nativeDetail(actions), CommunityCardActions(actions.actions, actions.onPhoto), modifier)
}

@Composable
private fun NativeCheckInDetail(data: NativeCheckInDetailPresentation, actions: CommunityCardActions, modifier: Modifier,
    overlay: @Composable () -> Unit = {}): Boolean {
    val host = LocalUIViewController.current.nativeScreenHost()?.takeIf { it.supportsNativeDetailBar } ?: return false
    val accounts = koinInject<SessionReader>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val detail = remember(host) { IosCheckInDetail(data, actions, IosCatalogueMedia(fetcher, Constants.BASE_URL), accounts) }
    val controller = remember(detail) { detailFactory(detail) }
    val theme by ThemeManager.themeMode.collectAsState()
    val onBack by rememberUpdatedState(actions.actions["back"])
    SideEffect {
        detail.update(data, actions)
        controller.overrideUserInterfaceStyle = theme.uiKitStyle
        host.configureDetailNavigation(data.title, data.dismissAllowed, { onBack?.invoke() }, data.backLabel)
    }
    DisposableEffect(detail, host) { onDispose { host.resetDetailNavigation(); detail.close() } }
    Box(modifier.fillMaxSize()) {
        NativeEditorControllerHost(controller, Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.BottomCenter)) { overlay() }
    }
    return true
}
