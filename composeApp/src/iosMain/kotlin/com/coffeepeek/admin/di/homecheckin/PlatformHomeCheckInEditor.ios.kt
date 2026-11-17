@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.homecheckin

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.homecheckin.impl.NativeHomeCheckInSession
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInPhotoPicker
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInPhotoPickerController
import com.coffeepeek.feature.settings.api.LanguagePort
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosHomeCheckInEditorController = staticCompositionLocalOf<(IosHomeCheckInEditor) -> UIViewController> {
    error("The iOS app must provide a home check-in editor controller")
}

@Composable
internal actual fun PlatformHomeCheckInEditor(owner: Any, preparationId: String?, generation: Int,
    allowed: Boolean, picker: HomeCheckInPhotoPicker, create: () -> NativeHomeCheckInSession,
    onClose: () -> Unit, onSaved: (String, Boolean) -> Unit, onDismissAllowed: (Boolean) -> Unit,
    modifier: Modifier, fallback: @Composable () -> Unit) {
    key(owner, preparationId, generation) {
        val session = remember { create() }
        HomeCheckInNativeHost(session, allowed, picker, onClose, onSaved, onDismissAllowed, modifier)
    }
}

@Composable
private fun HomeCheckInNativeHost(session: NativeHomeCheckInSession, allowed: Boolean,
    picker: HomeCheckInPhotoPicker, onClose: () -> Unit, onSaved: (String, Boolean) -> Unit,
    onDismissAllowed: (Boolean) -> Unit, modifier: Modifier) {
    val factory = LocalIosHomeCheckInEditorController.current
    val language = koinInject<LanguagePort>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val accounts = koinInject<SessionReader>()
    val photos = remember(session) { mutableStateOf<HomeCheckInPhotoPickerController?>(null) }
    val editor = remember(session) {
        IosHomeCheckInEditor(session, language, IosCatalogueMedia(fetcher, Constants.BASE_URL),
            CheckInHistorySession(accounts)) { photos.value }
    }
    val controller = remember(editor) { factory(editor) }
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    CompositionLocalProvider(LocalUIViewController provides controller) {
        val photoController = picker.rememberController()
        SideEffect { photos.value = photoController }
    }
    DisposableEffect(editor) { onDispose { photos.value = null; editor.close() } }
    HomeCheckInNativeEffects(session, allowed, onClose, onSaved, onDismissAllowed)
    NativeEditorControllerHost(controller, modifier.fillMaxSize())
}

@Composable
private fun HomeCheckInNativeEffects(session: NativeHomeCheckInSession, allowed: Boolean,
    onClose: () -> Unit, onSaved: (String, Boolean) -> Unit, onDismissAllowed: (Boolean) -> Unit) {
    val close by rememberUpdatedState(onClose)
    val saved by rememberUpdatedState(onSaved)
    val dismiss by rememberUpdatedState(onDismissAllowed)
    val access by rememberUpdatedState(allowed)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(session, allowed) { if (!allowed) session.revokeAccess() }
    LaunchedEffect(session) { session.state.collect { dismiss(it.status.dismissAllowed) } }
    LaunchedEffect(session, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            session.events.collect { event ->
                if (access && session.isCurrent()) when (event.kind) {
                    "close" -> close()
                    "saved" -> event.preparationId?.let { saved(it, event.visibilityUnconfirmed) }
                }
            }
        }
    }
}
