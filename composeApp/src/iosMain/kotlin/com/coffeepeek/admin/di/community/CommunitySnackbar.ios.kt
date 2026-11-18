@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.community

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.snackbar_dismiss
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.NativeEditorControllerHost
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import platform.UIKit.UIAccessibilityAnnouncementNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIViewController

object IosCommunityMessageControllers {
    internal lateinit var factory: (IosCommunityMessage) -> UIViewController
    fun configure(factory: (IosCommunityMessage) -> UIViewController) { this.factory = factory }
}

@Composable
internal actual fun CommunitySnackbar(state: SnackbarHostState, clearance: Dp) {
    // Material owns only the existing queue, timeout and transition lifecycle;
    // the message, controls and card are rendered by SwiftUI.
    SnackbarHost(state, Modifier.padding(bottom = clearance)) { data ->
        CommunityMessage(state, data)
    }
}

@Composable
private fun CommunityMessage(state: SnackbarHostState, data: SnackbarData) {
    val dismiss = stringResource(Res.string.snackbar_dismiss)
    val source = remember(state, data, dismiss) {
        IosCommunityMessage(data.visuals.message, data.visuals.actionLabel, dismiss,
            { state.currentSnackbarData === data }, data::dismiss, data::performAction)
    }
    val controller = remember(source) { IosCommunityMessageControllers.factory(source) }
    val height by source.height.collectAsState()
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    DisposableEffect(source) { onDispose { source.close() } }
    LaunchedEffect(source) { UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, source.text) }
    NativeEditorControllerHost(controller, Modifier.fillMaxWidth().height(height.toFloat().dp))
}
