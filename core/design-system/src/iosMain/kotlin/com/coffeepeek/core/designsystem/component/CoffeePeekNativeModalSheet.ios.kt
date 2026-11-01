@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import kotlin.math.abs
import kotlinx.cinterop.useContents
import org.jetbrains.compose.ui.tooling.preview.Preview
import platform.UIKit.*
import platform.darwin.NSObject

@Composable
actual fun CoffeePeekNativeModalSheet(
    onDismiss: () -> Unit, dismissEnabled: Boolean, maxHeightFraction: Float?,
    modifier: Modifier, content: @Composable ColumnScope.() -> Unit,
): Boolean {
    if ((UIDevice.currentDevice.systemVersion.substringBefore('.').toIntOrNull() ?: 0) < 16) return false
    val host = LocalUIViewController.current
    val dismiss = rememberUpdatedState(onDismiss)
    val body = rememberUpdatedState(content)
    val locals = rememberUpdatedState(currentCompositionLocalContext)
    val fraction = rememberUpdatedState(maxHeightFraction?.coerceIn(0.1f, 1f) ?: 1f)
    val expanded = rememberUpdatedState(LocalCoffeePeekSheetExpandToMaxHeight.current)
    val sheetModifier = rememberUpdatedState(modifier)
    val colors = MaterialTheme.colorScheme
    val sizing = remember(host) { coffeePeekSheetSizing(host) }
    val controller = remember(host) {
        ComposeUIViewController(configure = { opaque = true }) {
            CoffeePeekModalComposition(locals.value) {
                CompositionLocalProvider(
                    LocalCoffeePeekScreenInsets provides CoffeePeekScreenInsets(),
                    LocalCoffeePeekGlassHazeState provides null,
                    LocalCoffeePeekSheetExpandToMaxHeight provides false,
                ) {
                    CoffeePeekNativeSheetContent(sizing, fraction.value, sheetModifier.value, body.value)
                }
            }
        }.apply { modalPresentationStyle = UIModalPresentationPageSheet }
    }
    val delegate = remember { CoffeePeekSheetDelegate { dismiss.value() } }
    SideEffect {
        controller.modalInPresentation = !dismissEnabled
        controller.overrideUserInterfaceStyle = coffeePeekUserInterfaceStyle(colors.background)
        controller.view.backgroundColor = colors.surface.toUIColor()
        controller.view.tintColor = colors.primary.toUIColor()
    }
    CoffeePeekNativeSheetDetents(controller, sizing, fraction, expanded, delegate)
    CoffeePeekNativeSheetResize(controller, sizing.contentHeight, sizing.maximumHeight, fraction.value, expanded.value)
    CoffeePeekPresentNativeController(controller, host)
    return true
}

/** Application locals are inherited; focus, keyboard, window and interop belong to this controller. */
@Composable
private fun CoffeePeekModalComposition(parent: CompositionLocalContext, content: @Composable () -> Unit) {
    val owner = currentCompositionLocalContext
    CompositionLocalProvider(parent) {
        CompositionLocalProvider(owner, content)
    }
}

private fun coffeePeekSheetSizing(host: UIViewController): CoffeePeekSheetSizing {
    val window = host.view.window
    val screenHeight = (window?.bounds ?: UIScreen.mainScreen.bounds).useContents { size.height }
    val safeInsets = window?.safeAreaInsets?.useContents { top + bottom } ?: 0.0
    return CoffeePeekSheetSizing((screenHeight - safeInsets).coerceAtLeast(1.0))
}

@Composable
private fun CoffeePeekNativeSheetContent(
    sizing: CoffeePeekSheetSizing, fraction: Float, modifier: Modifier, content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))) {
            CoffeePeekContentSizedSheetLayout(
                maxContentHeight = (sizing.maximumHeight * fraction - 24.0).coerceAtLeast(1.0).dp,
                onContentHeightChanged = { pixels ->
                    val points = pixels / density.density + 24.0
                    if (abs(sizing.contentHeight - points) > 0.5) sizing.contentHeight = points
                },
                modifier = modifier.fillMaxSize().padding(top = 24.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun CoffeePeekNativeSheetDetents(
    controller: UIViewController, sizing: CoffeePeekSheetSizing,
    fraction: State<Float>, expanded: State<Boolean>, delegate: CoffeePeekSheetDelegate,
) {
    DisposableEffect(controller) {
        controller.sheetPresentationController?.apply {
            detents = listOf(UISheetPresentationControllerDetent.customDetentWithIdentifier("content") { context ->
                val maximum = context?.maximumDetentValue ?: sizing.maximumHeight
                if (maximum > 0 && abs(sizing.maximumHeight - maximum) > 0.5) sizing.maximumHeight = maximum
                coffeePeekNativeSheetDetentHeight(sizing.contentHeight, maximum, fraction.value, expanded.value)
            })
            prefersGrabberVisible = true
            prefersScrollingExpandsWhenScrolledToEdge = false
            prefersEdgeAttachedInCompactHeight = true
        }
        controller.presentationController?.delegate = delegate
        onDispose { controller.presentationController?.delegate = null }
    }
}

@Composable
private fun CoffeePeekNativeSheetResize(controller: UIViewController, height: Double, maximum: Double, fraction: Float, expanded: Boolean) {
    LaunchedEffect(controller, height, maximum, fraction, expanded) {
        if (!expanded && height > 0) controller.sheetPresentationController?.invalidateDetents()
    }
}

private class CoffeePeekSheetSizing(maximumHeight: Double) {
    var maximumHeight by mutableDoubleStateOf(maximumHeight)
    var contentHeight by mutableDoubleStateOf(0.0)
}

private class CoffeePeekSheetDelegate(val dismissed: () -> Unit) : NSObject(), UIAdaptivePresentationControllerDelegateProtocol {
    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) = dismissed()
}

@Composable
private fun CoffeePeekNativeSheetContentPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Box(Modifier.size(390.dp, 600.dp)) {
        CoffeePeekNativeSheetContent(CoffeePeekSheetSizing(600.0), 1f, Modifier) {
            Text("Sheet content", Modifier.padding(24.dp))
        }
    }
}

@Preview @Composable private fun CoffeePeekNativeSheetContentLightPreview() = CoffeePeekNativeSheetContentPreview(false)
@Preview @Composable private fun CoffeePeekNativeSheetContentDarkPreview() = CoffeePeekNativeSheetContentPreview(true)
