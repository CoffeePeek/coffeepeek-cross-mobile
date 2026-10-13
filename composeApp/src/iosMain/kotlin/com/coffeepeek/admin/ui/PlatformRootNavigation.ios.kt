@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.ui.component.dismissKeyboardOnBackgroundGestures
import com.coffeepeek.admin.ui.Navigator.isHandledByRootNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.UIKit.*
import platform.darwin.NSObject

private class BackGestureDelegate(private val navigation: UINavigationController) : NSObject(), UIGestureRecognizerDelegateProtocol {
    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean =
        navigation.viewControllers.size > 1 && navigation.transitionCoordinator == null && navigation.presentedViewController == null
}

@Composable
internal actual fun PlatformRootNavigation(events: Flow<Navigator.NavEvent>, appLink: Navigator.Screen?, consumeLink: () -> Unit, content: @Composable (Navigator.Screen, () -> Unit) -> Unit): Boolean {
    val body = rememberUpdatedState(content)
    val navigation = remember { UINavigationController().apply { setNavigationBarHidden(true, false) } }
    val gestureDelegate = remember(navigation) { BackGestureDelegate(navigation) }
    val scope = rememberCoroutineScope()
    val transitions = remember { Mutex() }
    suspend fun navigate(action: (Boolean) -> Unit) = transitions.withLock {
        while (navigation.transitionCoordinator != null) delay(16)
        navigation.view.endEditing(true)
        action(!UIAccessibilityIsReduceMotionEnabled())
        // Give UIKit a run-loop iteration to install its transition coordinator.
        delay(16)
    }
    fun controller(screen: Navigator.Screen): UIViewController = ComposeUIViewController {
        AppContent {
            Box(Modifier.fillMaxSize().dismissKeyboardOnBackgroundGestures()) {
                body.value(screen) {
                    scope.launch { navigate { animated ->
                        val root = navigation.viewControllers.first()
                        navigation.setViewControllers(listOf(root, controller(Navigator.Screen.Auth)), animated)
                    } }
                }
            }
        }
    }
    val root = remember(navigation) { controller(Navigator.Screen.Main) }
    UIKitViewController(factory = {
        navigation.setViewControllers(listOf(root), false)
        navigation.interactivePopGestureRecognizer?.delegate = gestureDelegate
        navigation
    }, modifier = Modifier.fillMaxSize(), properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative))
    LaunchedEffect(navigation) {
        events.collect { event ->
            // UIKit must finish a push/pop (including cancelled interactive pops) before the next.
            navigate { animated ->
            when (event) {
                Navigator.NavEvent.PopBack, Navigator.NavEvent.NavigateUp -> if (navigation.viewControllers.size > 1) navigation.popViewControllerAnimated(animated)
                is Navigator.NavEvent.NavigateTo -> if (event.screen.isHandledByRootNav()) {
                    if (event.screen == Navigator.Screen.Main) navigation.popToRootViewControllerAnimated(animated)
                    else navigation.pushViewController(controller(event.screen), animated)
                }
                else -> Unit
            }
            }
        }
    }
    LaunchedEffect(appLink) {
        appLink?.let {
            navigate { animated -> navigation.setViewControllers(listOf(root, controller(it)), animated) }
            consumeLink()
        }
    }
    DisposableEffect(navigation) {
        onDispose { navigation.interactivePopGestureRecognizer?.delegate = null }
    }
    return true
}
