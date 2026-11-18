@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui.dialogs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalUIViewController
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.loader_loading
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource
import com.coffeepeek.admin.di.system.IosGlobalDialog
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.admin.ui.component.PresentNativeController
import platform.UIKit.UIViewController

private lateinit var globalDialogFactory: (IosGlobalDialog) -> UIViewController

internal fun configureNativeGlobalDialogs(factory: (IosGlobalDialog) -> UIViewController) { globalDialogFactory = factory }

@Composable
internal actual fun PlatformGlobalLoading(text: String?): Boolean {
    val label = coffeePeekStringResource(Res.string.loader_loading)
    val model = remember(text, label) { IosGlobalDialog(false, label, text, "", {}) }
    val controller = remember(model) { globalDialogFactory(model) }
    GlobalDialogLifetime(model, controller)
    NativeEditorControllerHost(controller, Modifier.fillMaxSize())
    return true
}

@Composable
internal actual fun PlatformGlobalError(title: String, message: String, closeLabel: String, onDismiss: () -> Unit): Boolean {
    val dismiss = rememberUpdatedState(onDismiss)
    val model = remember(title, message, closeLabel) { IosGlobalDialog(true, title, message, closeLabel) { dismiss.value() } }
    val controller = remember(model) { globalDialogFactory(model) }
    GlobalDialogLifetime(model, controller)
    PresentNativeController(controller, LocalUIViewController.current)
    return true
}

@Composable
private fun GlobalDialogLifetime(model: IosGlobalDialog, controller: UIViewController) {
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    DisposableEffect(model) { onDispose { model.close() } }
}
