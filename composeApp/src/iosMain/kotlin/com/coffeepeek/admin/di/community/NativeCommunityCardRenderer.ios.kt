@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.community

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.community.api.CommunityCardActions
import com.coffeepeek.feature.community.api.CommunityCardPresentation
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

private lateinit var cardFactory: (IosCommunityCard) -> UIViewController

internal fun configureNativeCommunityCards(factory: (IosCommunityCard) -> UIViewController) { cardFactory = factory }

internal actual fun nativeCommunityCardRenderer(): NativeCommunityCardRenderer? = { data, actions, modifier ->
    NativeCommunityCard(data, actions, modifier)
}

@Composable
private fun NativeCommunityCard(data: CommunityCardPresentation, actions: CommunityCardActions, modifier: Modifier) {
    val accounts = koinInject<SessionReader>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val session by accounts.observeSession().collectAsState(initial = accounts.peekSession())
    key(data.key, session?.userId, accounts.accountGeneration()) {
        val card = remember { IosCommunityCard(data, actions, IosCatalogueMedia(fetcher, Constants.BASE_URL), accounts) }
        val controller = remember(card) { cardFactory(card) }
        val height by card.height.collectAsState()
        val theme by ThemeManager.themeMode.collectAsState()
        SideEffect { card.update(data, actions); controller.overrideUserInterfaceStyle = theme.uiKitStyle }
        DisposableEffect(card) { onDispose { card.close() } }
        NativeEditorControllerHost(controller, modifier.fillMaxWidth().height(height.toFloat().dp),
            interactionMode = UIKitInteropInteractionMode.Cooperative())
    }
}
