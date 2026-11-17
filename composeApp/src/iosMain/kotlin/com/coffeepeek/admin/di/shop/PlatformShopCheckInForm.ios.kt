@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.shop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.coffeepeek.admin.ui.component.NativeNavigationBar
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPickerController
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosShopCheckInEditorController = staticCompositionLocalOf<((IosShopCheckInEditor) -> UIViewController)?> { null }

@Composable
internal actual fun PlatformShopCheckInForm(form: ShopCheckInNativeForm): Boolean {
    val factory = LocalIosShopCheckInEditorController.current ?: return false
    val language = koinInject<LanguagePort>()
    val picker = remember(form.repository) { mutableStateOf<ShopCheckInPhotoPickerController?>(null) }
    val callbacks by rememberUpdatedState(form.callbacks)
    val editor = remember(form.repository) { IosShopCheckInEditor(form, language, { picker.value }, { callbacks }) }
    val controller = remember(editor) { factory(editor) }
    CompositionLocalProvider(LocalUIViewController provides controller) {
        val photos = form.photos.rememberController()
        SideEffect { picker.value = photos; editor.syncPicker() }
    }
    DisposableEffect(editor) { onDispose { picker.value = null; editor.close() } }
    NativeNavigationBar(form.shopName, editor::dismiss, customBack = true, action = null)
    UIKitViewController(factory = { controller }, modifier = Modifier.fillMaxSize(),
        properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true))
    return true
}
