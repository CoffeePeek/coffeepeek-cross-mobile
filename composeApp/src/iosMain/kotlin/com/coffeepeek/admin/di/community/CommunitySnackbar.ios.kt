@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.community

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.Dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.snackbar_dismiss
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import kotlinx.coroutines.delay
import platform.UIKit.*

object IosCommunityMessageControllers {
    internal lateinit var factory: (IosCommunityMessage) -> UIViewController
    fun configure(factory: (IosCommunityMessage) -> UIViewController) { this.factory = factory }
}

@Composable
internal actual fun CommunitySnackbar(state: SnackbarHostState, clearance: Dp) {
    val data = state.currentSnackbarData ?: return
    val timeout = when (data.visuals.duration) {
        SnackbarDuration.Short -> 4_000L
        SnackbarDuration.Long -> 10_000L
        SnackbarDuration.Indefinite -> Long.MAX_VALUE
    }
    val duration = LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(timeout,
        containsIcons = true, containsText = true, containsControls = true) ?: timeout
    LaunchedEffect(data, duration) {
        delay(duration)
        data.dismiss()
    }
    CommunityMessage(state, data, clearance)
}

@Composable
private fun CommunityMessage(state: SnackbarHostState, data: SnackbarData, clearance: Dp) {
    val dismiss = stringResource(Res.string.snackbar_dismiss)
    val source = remember(state, data, dismiss) {
        IosCommunityMessage(data.visuals.message, data.visuals.actionLabel, dismiss,
            { state.currentSnackbarData === data }, data::dismiss, data::performAction)
    }
    val controller = remember(source) { IosCommunityMessageControllers.factory(source) }
    val host = LocalUIViewController.current
    val height by source.height.collectAsState()
    val constraint = remember(controller) { controller.view.heightAnchor.constraintEqualToConstant(height) }
    val bottom = remember(controller, host) { controller.view.bottomAnchor.constraintEqualToAnchor(host.view.safeAreaLayoutGuide.bottomAnchor) }
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect {
        controller.overrideUserInterfaceStyle = theme.uiKitStyle
        constraint.constant = height
        bottom.constant = -clearance.value.toDouble() - 8.0
    }
    LaunchedEffect(controller, host) { attachNativeMessage(host, controller, bottom, constraint) }
    DisposableEffect(source, controller) {
        onDispose { source.close(); removeNativeMessage(controller) }
    }
    LaunchedEffect(source) { UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, source.text) }
}

private suspend fun attachNativeMessage(host: UIViewController, controller: UIViewController, bottom: NSLayoutConstraint,
    height: NSLayoutConstraint) {
    while (host.view.window == null) delay(16)
    host.addChildViewController(controller)
    val view = controller.view
    view.translatesAutoresizingMaskIntoConstraints = false
    host.view.addSubview(view)
    NSLayoutConstraint.activateConstraints(nativeMessageConstraints(host.view, view, bottom, height))
    controller.didMoveToParentViewController(host)
    view.alpha = if (UIAccessibilityIsReduceMotionEnabled()) 1.0 else 0.0
    UIView.animateWithDuration(0.2) { view.alpha = 1.0 }
}

private fun nativeMessageConstraints(parent: UIView, view: UIView, bottom: NSLayoutConstraint, height: NSLayoutConstraint) = listOf(
    view.centerXAnchor.constraintEqualToAnchor(parent.centerXAnchor),
    view.widthAnchor.constraintLessThanOrEqualToConstant(560.0),
    view.leadingAnchor.constraintGreaterThanOrEqualToAnchor(parent.safeAreaLayoutGuide.leadingAnchor, 16.0),
    view.trailingAnchor.constraintLessThanOrEqualToAnchor(parent.safeAreaLayoutGuide.trailingAnchor, -16.0),
    view.widthAnchor.constraintEqualToAnchor(parent.safeAreaLayoutGuide.widthAnchor, -32.0).apply { priority = 999f },
    bottom,
    height,
)

private fun removeNativeMessage(controller: UIViewController) {
    if (controller.parentViewController == null) return
    if (UIAccessibilityIsReduceMotionEnabled() || controller.view.window == null) return detachNativeMessage(controller)
    UIView.animateWithDuration(0.15, animations = { controller.view.alpha = 0.0 },
        completion = { detachNativeMessage(controller) })
}

private fun detachNativeMessage(controller: UIViewController) {
    if (controller.parentViewController == null) return
    controller.willMoveToParentViewController(null)
    controller.view.removeFromSuperview()
    controller.removeFromParentViewController()
}
