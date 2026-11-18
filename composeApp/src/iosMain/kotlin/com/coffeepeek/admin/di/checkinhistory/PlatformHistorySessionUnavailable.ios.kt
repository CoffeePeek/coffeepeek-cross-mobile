@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.history_session_unavailable
import coffeepeek.composeapp.generated.resources.history_session_retry
import coffeepeek.composeapp.generated.resources.history_session_back
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import platform.UIKit.UIViewController

object IosHistoryFailureControllers {
    internal lateinit var factory: (IosHistoryFailure) -> UIViewController
    fun configure(factory: (IosHistoryFailure) -> UIViewController) { this.factory = factory }
}

@Composable
internal actual fun PlatformHistorySessionUnavailable(onRetry: () -> Unit, onBack: () -> Unit): Boolean {
    val message = stringResource(Res.string.history_session_unavailable)
    val retryLabel = stringResource(Res.string.history_session_retry)
    val backLabel = stringResource(Res.string.history_session_back)
    val retry = rememberUpdatedState(onRetry)
    val back = rememberUpdatedState(onBack)
    val source = remember(message, retryLabel, backLabel) {
        IosHistoryFailure(message, retryLabel, backLabel, { retry.value() }, { back.value() })
    }
    val controller = remember(source) { IosHistoryFailureControllers.factory(source) }
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    DisposableEffect(source) { onDispose { source.close() } }
    NativeEditorControllerHost(controller, Modifier.fillMaxSize())
    return true
}
