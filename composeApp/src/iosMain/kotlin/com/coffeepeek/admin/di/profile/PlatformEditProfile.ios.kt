@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.admin.ui.component.NativeNavigationBar
import com.coffeepeek.admin.utils.PhotoPickerController
import com.coffeepeek.admin.utils.rememberPhotoPicker
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.impl.NativeEditProfileSession
import com.coffeepeek.feature.settings.api.LanguagePort
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

private lateinit var editProfileControllerFactory: (IosEditProfile) -> UIViewController

/** Startup configuration shared by all native route Compose hosts. */
internal fun configureNativeEditProfile(factory: (IosEditProfile) -> UIViewController) {
    editProfileControllerFactory = factory
}

internal val LocalIosEditProfileController = staticCompositionLocalOf<(IosEditProfile) -> UIViewController> {
    check(::editProfileControllerFactory.isInitialized) { "The iOS app must provide an edit profile controller" }
    editProfileControllerFactory
}

@Composable
internal actual fun PlatformEditProfile(session: NativeEditProfileSession, onBack: () -> Unit,
    onDeletion: () -> Unit, modifier: Modifier): Boolean {
    val factory = LocalIosEditProfileController.current
    val language = koinInject<LanguagePort>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val photos = remember(session) { mutableStateOf<PhotoPickerController?>(null) }
    val back by rememberUpdatedState(onBack)
    val deletion by rememberUpdatedState(onDeletion)
    val editor = remember(session) {
        IosEditProfile(session, language, IosCatalogueMedia(fetcher, Constants.BASE_URL),
            { photos.value }, { back() }, { deletion() })
    }
    val controller = remember(editor) { factory(editor) }
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    CompositionLocalProvider(LocalUIViewController provides controller) {
        val picker = rememberPhotoPicker(1, editor::photoLoading) { images ->
            images.firstOrNull()?.let { session.avatarPicked(ProfileAvatarImage(it.fileName, it.contentType, it.bytes)) }
        }
        SideEffect { photos.value = picker }
    }
    DisposableEffect(editor) { onDispose { photos.value = null; editor.close() } }
    EditProfileNativeEffects(session, onBack)
    NativeNavigationBar("", editor::back, customBack = true, action = null)
    NativeEditorControllerHost(controller, modifier.fillMaxSize())
    return true
}

@Composable
private fun EditProfileNativeEffects(session: NativeEditProfileSession, onBack: () -> Unit) {
    val back by rememberUpdatedState(onBack)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(session, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            session.saved.collect { if (session.isCurrent()) back() }
        }
    }
}
