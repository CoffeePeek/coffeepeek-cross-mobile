@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import kotlinx.coroutines.delay
import platform.UIKit.*

/** Keep status above the native navigation hierarchy without presenting another modal controller. */
@Composable
internal actual fun NativeOperationStatus(message: String): Boolean {
    val host = LocalUIViewController.current
    val colors = MaterialTheme.colorScheme
    val container = remember(host) { UIView().apply {
        translatesAutoresizingMaskIntoConstraints = false
        userInteractionEnabled = false
        layer.cornerRadius = CpDimens.radius2xl.value.toDouble()
        layer.borderWidth = 1.0
        isAccessibilityElement = true
    } }
    val spinner = remember(container) { UIActivityIndicatorView().apply {
        translatesAutoresizingMaskIntoConstraints = false
        startAnimating()
    } }
    val label = remember(container) { UILabel().apply {
        translatesAutoresizingMaskIntoConstraints = false
        font = UIFont.preferredFontForTextStyle(UIFontTextStyleSubheadline)
        adjustsFontForContentSizeCategory = true
        numberOfLines = 0
    } }
    SideEffect {
        container.overrideUserInterfaceStyle = colors.uiKitStyle
        container.backgroundColor = colors.surface.toUIColor()
        container.layer.borderColor = colors.outlineVariant.toUIColor().CGColor
        container.accessibilityLabel = message
        spinner.color = colors.primary.toUIColor()
        label.textColor = colors.onSurface.toUIColor()
        label.text = message
    }
    LaunchedEffect(host, container) {
        while (host.view.window == null || host.transitionCoordinator != null) delay(16)
        val view = checkNotNull(host.view.window)
        container.addSubview(spinner)
        container.addSubview(label)
        view.addSubview(container)
        NSLayoutConstraint.activateConstraints(listOf(
            container.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor),
            container.leadingAnchor.constraintGreaterThanOrEqualToAnchor(view.safeAreaLayoutGuide.leadingAnchor, 16.0),
            container.trailingAnchor.constraintLessThanOrEqualToAnchor(view.safeAreaLayoutGuide.trailingAnchor, -16.0),
            container.bottomAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.bottomAnchor, -(CpDimens.floatingNavContentClearance + CpDimens.spacing4).value.toDouble()),
            container.widthAnchor.constraintLessThanOrEqualToConstant(440.0),
            container.heightAnchor.constraintGreaterThanOrEqualToConstant(52.0),
            spinner.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor, 20.0),
            spinner.centerYAnchor.constraintEqualToAnchor(container.centerYAnchor),
            label.leadingAnchor.constraintEqualToAnchor(spinner.trailingAnchor, 12.0),
            label.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor, -20.0),
            label.topAnchor.constraintEqualToAnchor(container.topAnchor, 16.0),
            label.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, -16.0),
        ))
    }
    LaunchedEffect(message) { UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, message) }
    DisposableEffect(container) {
        onDispose { spinner.stopAnimating(); container.removeFromSuperview() }
    }
    return true
}
