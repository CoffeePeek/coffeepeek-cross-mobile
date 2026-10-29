@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui

import org.koin.compose.koinInject
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.business_tab_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.theme.toUIColor
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.theme.ThemeManager
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.coffeepeek.admin.feature.coffee.ui.CoffeeListScreen
import com.coffeepeek.admin.feature.community.ui.CommunityScreen
import com.coffeepeek.admin.ui.Navigator.isHandledByRootNav
import com.coffeepeek.admin.ui.screen.main.DiscoveryScreenRenderer
import com.coffeepeek.admin.ui.screen.main.CheckInTabContent
import com.coffeepeek.admin.ui.screen.profile.ProfileScreen
import com.coffeepeek.admin.ui.screen.profile.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.cinterop.ObjCSignatureOverride
import platform.UIKit.*
import platform.darwin.NSObject

internal class NavigationDelegate : NSObject(), UINavigationControllerDelegateProtocol, UIGestureRecognizerDelegateProtocol {
    private var previousTop: UIViewController? = null
    var navigation: UINavigationController? = null
    var onStackChanged: (() -> Unit)? = null
    @ObjCSignatureOverride
    override fun navigationController(navigationController: UINavigationController, willShowViewController: UIViewController, animated: Boolean) {
        navigationController.setNavigationBarHidden(true, animated)
    }

    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean {
        val nav = navigation ?: return false
        return nav.viewControllers.size > 1 && nav.transitionCoordinator == null && nav.presentedViewController == null &&
            (nav.topViewController as? NativeScreenController)?.handlesBackLocally != true
    }

    @ObjCSignatureOverride
    override fun navigationController(navigationController: UINavigationController, didShowViewController: UIViewController, animated: Boolean) {
        // Hidden UIKit headers must keep the system edge-back gesture after each transition.
        navigationController.interactivePopGestureRecognizer?.apply {
            delegate = this@NavigationDelegate
            enabled = navigationController.viewControllers.size > 1
        }
        val previous = previousTop as? NativeScreenController
        if (previous?.route == Navigator.Screen.CreateCheckIn && navigationController.viewControllers.none { it == previous }) {
            com.coffeepeek.admin.feature.checkin.api.CheckInFlowHandoff.cancelFromBack()
        }
        previousTop = didShowViewController
        onStackChanged?.invoke()
    }
}

internal class TabDelegate : NSObject(), UITabBarControllerDelegateProtocol {
    var onTabChanged: (() -> Unit)? = null
    var onTabReselected: ((UIViewController) -> Unit)? = null
    @ObjCSignatureOverride
    override fun tabBarController(tabBarController: UITabBarController, shouldSelectViewController: UIViewController): Boolean {
        tabBarController.view.endEditing(true)
        val navigation = shouldSelectViewController as? UINavigationController
        if (navigation != null && tabBarController.selectedViewController == navigation && navigation.viewControllers.size == 1) {
            onTabReselected?.invoke(navigation)
        }
        return true
    }

    @ObjCSignatureOverride
    override fun tabBarController(tabBarController: UITabBarController, didSelectViewController: UIViewController) {
        onTabChanged?.invoke()
    }
}

/** Keeps the visible UIKit stack when SwiftUI recreates the Compose host on foregrounding. */
private object NavigationMemory {
    var generation = 0
    var selectedTab = 0
    var stacks: List<List<Navigator.Screen>> = List(6) { emptyList() }

    fun save(tabs: UITabBarController, navigations: List<UINavigationController>, visibleTabIndices: List<Int>) {
        selectedTab = visibleTabIndices[tabs.selectedIndex.toInt().coerceIn(visibleTabIndices.indices)]
        stacks = navigations.map { navigation ->
            navigation.viewControllers.drop(1).mapNotNull { (it as? NativeScreenController)?.route }
        }
    }

    fun clear() {
        generation++
        selectedTab = 0
        stacks = List(6) { emptyList() }
    }
}

internal actual fun clearPlatformNavigationState() = NavigationMemory.clear()

@Composable
internal actual fun PlatformRootNavigation(
    isLoggedIn: Boolean,
    events: Flow<Navigator.NavEvent>, appLink: Navigator.Screen?, consumeLink: () -> Unit,
    content: @Composable (Navigator.Screen, resetToLogin: () -> Unit) -> Unit,
): Boolean {
    val body = rememberUpdatedState(content)
    val scope = rememberCoroutineScope()
    val transitions = remember { Mutex() }
    val tabs = remember { UITabBarController() }
    val tabDelegate = remember { TabDelegate() }
    val tabReselections = remember { MainTabReselections() }
    val navigationDelegates = remember { List(6) { NavigationDelegate() } }
    val navigations = remember { List(6) { UINavigationController() } }
    val businessMode = koinInject<com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry>().Session().businessMode
    val visibleTabIndices = remember(isLoggedIn, businessMode) { mainTabScreens(isLoggedIn, businessMode).map { requireNotNull(nativeTabIndex(it)) } }
    val restoredTab = remember { NavigationMemory.selectedTab }
    val restoredStacks = remember { NavigationMemory.stacks }
    val memoryGeneration = remember { NavigationMemory.generation }
    val pendingTab by Navigator.pendingTabSelection.collectAsState()

    fun saveNavigation() {
        if (NavigationMemory.generation == memoryGeneration) NavigationMemory.save(tabs, navigations, visibleTabIndices)
    }

    suspend fun navigate(block: () -> Unit) = transitions.withLock {
        while (navigations.any { it.transitionCoordinator != null }) delay(16)
        tabs.view.endEditing(true)
        block()
        delay(16)
    }

    fun currentNavigation() = navigations[visibleTabIndices[tabs.selectedIndex.toInt().coerceIn(visibleTabIndices.indices)]]
    fun selectTabRoot(destination: Navigator.Screen) {
        nativeTabIndex(destination)?.let { index ->
            val visibleIndex = visibleTabIndices.indexOf(index).takeIf { it >= 0 } ?: return
            tabs.selectedIndex = visibleIndex.toULong()
            // Programmatic selection means opening a tab's entry point (e.g. shop -> map).
            // Direct tab-bar taps still retain that tab's current stack.
            navigations[index].popToRootViewControllerAnimated(false)
            saveNavigation()
        }
    }
    fun controller(screen: Navigator.Screen): UIViewController = NativeScreenController(
        route = screen,
    ) {
        body.value(screen) {
            scope.launch { navigate {
                val navigation = currentNavigation()
                navigation.setViewControllers(listOf(navigation.viewControllers.first(), controller(Navigator.Screen.Auth).apply { hidesBottomBarWhenPushed = true }), !UIAccessibilityIsReduceMotionEnabled())
                saveNavigation()
            } }
        }
    }

    val businessTitle = stringResource(Res.string.business_tab_title)
    UIKitViewController(factory = {
        val labels = listOf("Поиск", "Кофе", "Лента", "Профиль", "Настройки", businessTitle)
        val symbols = listOf("magnifyingglass", "cup.and.saucer", "person.2", "person.crop.circle", "gearshape", "building.2")
        navigations.indices.forEach { index ->
            val navigation = navigations[index]
            val root = NativeScreenController(labels[index], analyticsPage = listOf("discovery", "coffees", "feed", "profile", "settings", "business_ownership")[index], isTabRoot = true) {
                when (index) {
                    0 -> koinInject<DiscoveryScreenRenderer>().Tab(onMapExpandedChange = {})
                    1 -> CheckInTabContent { CoffeeListScreen(scrollToTopRequests = tabReselections.coffee) }
                    2 -> CommunityScreen(
                        onCreateCheckIn = { Navigator.navigate(Navigator.Screen.CreateCheckIn) },
                        notificationsAction = { koinInject<com.coffeepeek.admin.feature.notifications.ui.NotificationsRenderer>().Bell() },
                    )
                    3 -> ProfileScreen()
                    4 -> SettingsScreen()
                    5 -> koinInject<com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry>().Tab()
                }
            }
            navigation.setNavigationBarHidden(true, false)
            navigation.setViewControllers(
                listOf(root) + restoredStacks[index].map { screen ->
                    controller(screen).apply { hidesBottomBarWhenPushed = true }
                },
                false,
            )
            navigation.tabBarItem = UITabBarItem(title = labels[index], image = UIImage.systemImageNamed(symbols[index]), tag = index.toLong())
            navigationDelegates[index].navigation = navigation
            navigationDelegates[index].onStackChanged = { saveNavigation() }
            navigation.delegate = navigationDelegates[index]
            navigation.interactivePopGestureRecognizer?.delegate = navigationDelegates[index]
        }
        tabs.setViewControllers(visibleTabIndices.map(navigations::get), false)
        tabs.selectedIndex = visibleTabIndices.indexOf(restoredTab).coerceAtLeast(0).toULong()
        tabs.customizableViewControllers = null
        tabs.delegate = tabDelegate
        tabDelegate.onTabChanged = { saveNavigation() }
        tabDelegate.onTabReselected = { navigation ->
            tabReselections.onTabPressed(Navigator.Screen.CoffeeTab, isSelected = navigation == navigations[1])
        }
        tabs
    }, modifier = Modifier.fillMaxSize(), properties = UIKitInteropProperties(
        interactionMode = UIKitInteropInteractionMode.NonCooperative,
        isNativeAccessibilityEnabled = true,
        // This full-screen opaque host stays below the app's Compose overlays.
        placedAsOverlay = false,
    ))

    var previousTabIndices by remember { mutableStateOf(visibleTabIndices) }
    LaunchedEffect(visibleTabIndices) {
        if (previousTabIndices == visibleTabIndices) return@LaunchedEffect
        previousTabIndices = visibleTabIndices
        navigations.forEach { it.popToRootViewControllerAnimated(false) }
        tabs.setViewControllers(visibleTabIndices.map(navigations::get), false)
        tabs.selectedIndex = 0u
        saveNavigation()
    }
    val colors = MaterialTheme.colorScheme
    val themeMode by ThemeManager.themeMode.collectAsState()
    SideEffect {
        navigationDelegates.forEach { it.onStackChanged = { saveNavigation() } }
        tabDelegate.onTabChanged = { saveNavigation() }
        tabs.overrideUserInterfaceStyle = themeMode.uiKitStyle
        tabs.view.backgroundColor = colors.background.toUIColor()
        tabs.tabBar.tintColor = colors.primary.toUIColor()
        tabs.tabBar.unselectedItemTintColor = colors.onSurfaceVariant.toUIColor()
        navigations.forEach { it.view.backgroundColor = colors.background.toUIColor() }
    }
    LaunchedEffect(tabs, visibleTabIndices) {
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
                        tabIndex != null -> visibleTabIndices.indexOf(tabIndex).takeIf { it >= 0 }?.let { tabs.selectedIndex = it.toULong() }
                        event.screen.isHandledByRootNav() -> {
                            val stack = navigation.viewControllers.map { (it as? NativeScreenController)?.route }
                            val existing = navigation.viewControllers.getOrNull(existingProfileIndex(stack, event.screen)) as? UIViewController
                            if (existing != null) navigation.popToViewController(existing, animated)
                            else navigation.pushViewController(controller(event.screen).apply { hidesBottomBarWhenPushed = true }, animated)
                        }
                    }
                }
            }
            saveNavigation()
        } }
    }
    LaunchedEffect(pendingTab, visibleTabIndices) {
        pendingTab?.let { destination ->
            navigate { selectTabRoot(destination) }
            Navigator.consumeTabSelection()
        }
    }
    LaunchedEffect(appLink, visibleTabIndices) {
        appLink?.let { destination ->
            navigate {
                if (nativeTabIndex(destination) != null) selectTabRoot(destination)
                else {
                    val navigation = currentNavigation()
                    navigation.setViewControllers(listOf(navigation.viewControllers.first(), controller(destination).apply { hidesBottomBarWhenPushed = true }), !UIAccessibilityIsReduceMotionEnabled())
                }
                saveNavigation()
            }
            consumeLink()
        }
    }
    DisposableEffect(tabs) {
        onDispose {
            tabs.delegate = null
            tabDelegate.onTabChanged = null
            tabDelegate.onTabReselected = null
            navigations.forEach { it.delegate = null; it.interactivePopGestureRecognizer?.delegate = null }
            navigationDelegates.forEach { it.navigation = null; it.onStackChanged = null }
        }
    }
    return true
}
