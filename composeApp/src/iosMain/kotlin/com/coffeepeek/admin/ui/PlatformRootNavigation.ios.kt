@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.coffeepeek.admin.feature.coffee.ui.CoffeeListScreen
import com.coffeepeek.admin.feature.community.ui.CommunityScreen
import com.coffeepeek.admin.ui.Navigator.isHandledByRootNav
import com.coffeepeek.admin.ui.screen.main.DiscoveryTabContent
import com.coffeepeek.admin.ui.screen.profile.ProfileScreen
import com.coffeepeek.admin.ui.screen.profile.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.UIKit.*
import platform.darwin.NSObject

private class NavigationDelegate : NSObject(), UINavigationControllerDelegateProtocol, UIGestureRecognizerDelegateProtocol {
    var navigation: UINavigationController? = null
    override fun navigationController(navigationController: UINavigationController, willShowViewController: UIViewController, animated: Boolean) {
        val show = (willShowViewController as? NativeScreenController)?.usesNavigationBar == true
        navigationController.setNavigationBarHidden(!show, animated)
    }

    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean {
        val nav = navigation ?: return false
        return nav.viewControllers.size > 1 && nav.transitionCoordinator == null && nav.presentedViewController == null &&
            (nav.topViewController as? NativeScreenController)?.handlesBackLocally != true
    }
}

private class TabDelegate : NSObject(), UITabBarControllerDelegateProtocol {
    override fun tabBarController(tabBarController: UITabBarController, shouldSelectViewController: UIViewController): Boolean {
        tabBarController.view.endEditing(true)
        return true
    }
}

@Composable
internal actual fun PlatformRootNavigation(
    events: Flow<Navigator.NavEvent>, appLink: Navigator.Screen?, consumeLink: () -> Unit,
    content: @Composable (Navigator.Screen, resetToLogin: () -> Unit) -> Unit,
): Boolean {
    val body = rememberUpdatedState(content)
    val scope = rememberCoroutineScope()
    val transitions = remember { Mutex() }
    val tabs = remember { UITabBarController() }
    val tabDelegate = remember { TabDelegate() }
    val navigationDelegates = remember { List(5) { NavigationDelegate() } }
    val navigations = remember { List(5) { UINavigationController() } }
    val pendingTab by Navigator.pendingTabSelection.collectAsState()

    suspend fun navigate(block: () -> Unit) = transitions.withLock {
        while (navigations.any { it.transitionCoordinator != null }) delay(16)
        tabs.view.endEditing(true)
        block()
        delay(16)
    }

    fun currentNavigation() = navigations[tabs.selectedIndex.toInt().coerceIn(navigations.indices)]
    fun selectTabRoot(destination: Navigator.Screen) {
        nativeTabIndex(destination)?.let { index ->
            tabs.selectedIndex = index.toULong()
            // Programmatic selection means opening a tab's entry point (e.g. shop -> map).
            // Direct tab-bar taps still retain that tab's current stack.
            navigations[index].popToRootViewControllerAnimated(false)
        }
    }
    fun controller(screen: Navigator.Screen): UIViewController = NativeScreenController {
        body.value(screen) {
            scope.launch { navigate {
                val navigation = currentNavigation()
                navigation.setViewControllers(listOf(navigation.viewControllers.first(), controller(Navigator.Screen.Auth).apply { hidesBottomBarWhenPushed = true }), !UIAccessibilityIsReduceMotionEnabled())
            } }
        }
    }

    UIKitViewController(factory = {
        val labels = listOf("Поиск", "Кофе", "Лента", "Профиль", "Настройки")
        val symbols = listOf("magnifyingglass", "cup.and.saucer", "person.2", "person.crop.circle", "gearshape")
        navigations.forEachIndexed { index, navigation ->
            val root = NativeScreenController(labels[index]) {
                when (index) {
                    0 -> DiscoveryTabContent(onMapExpandedChange = {})
                    1 -> CoffeeListScreen()
                    2 -> CommunityScreen(onCreateCheckIn = { Navigator.navigate(Navigator.Screen.CreateCheckIn) })
                    3 -> ProfileScreen()
                    4 -> SettingsScreen()
                }
            }
            navigation.setNavigationBarHidden(true, false)
            navigation.setViewControllers(listOf(root), false)
            navigation.tabBarItem = UITabBarItem(title = labels[index], image = UIImage.systemImageNamed(symbols[index]), tag = index.toLong())
            navigationDelegates[index].navigation = navigation
            navigation.delegate = navigationDelegates[index]
            navigation.interactivePopGestureRecognizer?.delegate = navigationDelegates[index]
        }
        tabs.setViewControllers(navigations, false)
        tabs.customizableViewControllers = null
        tabs.delegate = tabDelegate
        tabs
    }, modifier = Modifier.fillMaxSize(), properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative))

    val colors = MaterialTheme.colorScheme
    SideEffect {
        tabs.overrideUserInterfaceStyle = if (colors.background.luminance() < .5f) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
        tabs.tabBar.tintColor = UIColor.colorWithRed(colors.primary.red.toDouble(), colors.primary.green.toDouble(), colors.primary.blue.toDouble(), 1.0)
    }
    LaunchedEffect(tabs) {
        events.collect { event -> navigate {
            val navigation = currentNavigation()
            val animated = !UIAccessibilityIsReduceMotionEnabled()
            when (event) {
                Navigator.NavEvent.PopBack, Navigator.NavEvent.NavigateUp -> if (navigation.viewControllers.size > 1) navigation.popViewControllerAnimated(animated)
                is Navigator.NavEvent.SelectTab -> selectTabRoot(event.tab)
                is Navigator.NavEvent.NavigateTo -> {
                    val tabIndex = nativeTabIndex(event.screen)
                    when {
                        event.screen == Navigator.Screen.Main -> navigation.popToRootViewControllerAnimated(animated)
                        tabIndex != null -> tabs.selectedIndex = tabIndex.toULong()
                        event.screen.isHandledByRootNav() -> navigation.pushViewController(controller(event.screen).apply { hidesBottomBarWhenPushed = true }, animated)
                    }
                }
            }
        } }
    }
    LaunchedEffect(pendingTab) {
        pendingTab?.let { destination ->
            navigate { selectTabRoot(destination) }
            Navigator.consumeTabSelection()
        }
    }
    LaunchedEffect(appLink) {
        appLink?.let { destination ->
            navigate {
                if (nativeTabIndex(destination) != null) selectTabRoot(destination)
                else {
                    val navigation = currentNavigation()
                    navigation.setViewControllers(listOf(navigation.viewControllers.first(), controller(destination).apply { hidesBottomBarWhenPushed = true }), !UIAccessibilityIsReduceMotionEnabled())
                }
            }
            consumeLink()
        }
    }
    DisposableEffect(tabs) {
        onDispose {
            tabs.delegate = null
            navigations.forEach { it.delegate = null; it.interactivePopGestureRecognizer?.delegate = null }
            navigationDelegates.forEach { it.navigation = null }
        }
    }
    return true
}
