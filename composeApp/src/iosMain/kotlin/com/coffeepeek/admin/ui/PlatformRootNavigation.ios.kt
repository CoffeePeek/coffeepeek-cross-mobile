@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.ui

import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground

import org.koin.compose.koinInject
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.business_tab_title
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import coffeepeek.composeapp.generated.resources.navigation_search
import coffeepeek.composeapp.generated.resources.navigation_feed
import coffeepeek.composeapp.generated.resources.navigation_profile
import coffeepeek.composeapp.generated.resources.navigation_settings
import coffeepeek.composeapp.generated.resources.check_in_create_actions
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
import com.coffeepeek.admin.ui.screen.community.CommunityScreenRenderer
import com.coffeepeek.admin.ui.Navigator.isHandledByRootNav
import com.coffeepeek.admin.ui.screen.main.DiscoveryScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.OwnProfileScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.SettingsScreenRenderer
import com.coffeepeek.admin.ui.screen.checkins.CheckInCreateHost
import com.coffeepeek.admin.analytics.TrackNativeCreateActionExposure
import com.coffeepeek.core.session.SessionReader
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
            com.coffeepeek.admin.di.shop.CheckInFlowHandoff.cancelFromBack()
        }
        previousTop = didShowViewController
        navigationController.viewControllers.forEach { (it as? NativeScreenController)?.syncBottomClearance() }
        onStackChanged?.invoke()
    }
}

internal class TabDelegate : NSObject(), UITabBarControllerDelegateProtocol {
    var onTabChanged: (() -> Unit)? = null
    var onTabReselected: ((UIViewController) -> Unit)? = null
    var createNavigation: UINavigationController? = null
    var onCreateRequested: (() -> Unit)? = null
    @ObjCSignatureOverride
    override fun tabBarController(tabBarController: UITabBarController, shouldSelectViewController: UIViewController): Boolean {
        tabBarController.view.endEditing(true)
        if (shouldSelectViewController == createNavigation) {
            onCreateRequested?.invoke()
            return false
        }
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
    private var saved = SavedNavigation()

    fun restore(isLoggedIn: Boolean) = saved.takeIf { it.isLoggedIn == isLoggedIn } ?: SavedNavigation()

    fun save(tabs: UITabBarController, navigations: List<UINavigationController>, visibleTabIndices: List<Int>, isLoggedIn: Boolean) {
        val selectedTab = visibleTabIndices[tabs.selectedIndex.toInt().coerceIn(visibleTabIndices.indices)]
        val stacks = navigations.map { navigation ->
            navigation.viewControllers.drop(1).mapNotNull { (it as? NativeScreenController)?.route }
        }
        saved = SavedNavigation(isLoggedIn, selectedTab, stacks)
    }

    fun clear() {
        generation++
        saved = SavedNavigation()
    }
}

private data class SavedNavigation(
    val isLoggedIn: Boolean? = null,
    val selectedTab: Int = 0,
    val stacks: List<List<Navigator.Screen>> = List(5) { emptyList() },
)

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
    val navigationDelegates = remember { List(5) { NavigationDelegate() } }
    val navigations = remember { List(5) { UINavigationController() } }
    val createNavigation = remember { UINavigationController(rootViewController = UIViewController()) }
    var createVisible by remember { mutableStateOf(false) }
    val tabBarVisibility = remember(tabs) { NativeTabBarVisibility(tabs, navigations[0]) { createVisible = it } }
    val businessMode = koinInject<com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry>().Session().businessMode
    val visibleTabIndices = remember(isLoggedIn, businessMode) { nativeRootTabIndices(isLoggedIn, businessMode) }
    val restoredNavigation = remember { NavigationMemory.restore(isLoggedIn) }
    val memoryGeneration = remember { NavigationMemory.generation }
    val pendingTab by Navigator.pendingTabSelection.collectAsState()
    var refreshCommunity by remember { mutableStateOf<(() -> Unit)?>(null) }
    NativeCreateAction(tabDelegate, refreshCommunity, createVisible)

    fun saveNavigation() {
        tabBarVisibility.sync()
        if (NavigationMemory.generation == memoryGeneration) NavigationMemory.save(tabs, navigations, visibleTabIndices, isLoggedIn)
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
    val createTitle = stringResource(Res.string.check_in_create_actions)
    val labels = listOf(stringResource(Res.string.navigation_search),
        stringResource(Res.string.navigation_feed), stringResource(Res.string.navigation_profile),
        stringResource(Res.string.navigation_settings), businessTitle)
    UIKitViewController(factory = {
        val symbols = listOf("magnifyingglass", "person.2", "person.crop.circle", "gearshape", "building.2")
        navigations.indices.forEach { index ->
            val navigation = navigations[index]
            val root = NativeScreenController(labels[index], analyticsPage = listOf("discovery", "feed", "profile", "settings", "business_ownership")[index], isTabRoot = true) {
                NativeRootTabContent(index, isLoggedIn, tabBarVisibility, tabReselections) { refreshCommunity = it }
            }
            navigation.setNavigationBarHidden(true, false)
            navigation.setViewControllers(
                listOf(root) + restoredNavigation.stacks[index].map { screen ->
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
        installRootTabsWithCreate(tabs, navigations, createNavigation, visibleTabIndices, restoredNavigation.selectedTab, createTitle)
        tabs.delegate = tabDelegate
        tabDelegate.createNavigation = createNavigation
        tabDelegate.onTabChanged = { saveNavigation() }
        tabDelegate.onTabReselected = { navigation ->
            tabReselections.onTabPressed(Navigator.Screen.FeedTab, isSelected = navigation == navigations[0])
        }
        tabs
    }, modifier = Modifier.fillMaxSize(), update = {
        updateNativeTabLabels(navigations, labels)
        createNavigation.tabBarItem.title = createTitle
    }, properties = UIKitInteropProperties(
        interactionMode = UIKitInteropInteractionMode.NonCooperative,
        isNativeAccessibilityEnabled = true,
        // This full-screen opaque host stays below the app's Compose overlays.
        placedAsOverlay = false,
    ))
    SideEffect { tabBarVisibility.sync() }

    var previousTabIndices by remember { mutableStateOf(visibleTabIndices) }
    LaunchedEffect(visibleTabIndices) {
        if (previousTabIndices == visibleTabIndices) return@LaunchedEffect
        previousTabIndices = visibleTabIndices
        navigations.forEach { it.popToRootViewControllerAnimated(false) }
        tabs.setViewControllers(visibleTabIndices.map { if (it == 5) createNavigation else navigations[it] }, false)
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
        tabs.view.tintColor = colors.coffeePeekAccentForeground.toUIColor()
        tabs.tabBar.tintColor = colors.coffeePeekAccentForeground.toUIColor()
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
            tabBarVisibility.dispose()
            tabs.delegate = null
            tabDelegate.onTabChanged = null
            tabDelegate.onTabReselected = null
            tabDelegate.onCreateRequested = null
            tabDelegate.createNavigation = null
            navigations.forEach { it.delegate = null; it.interactivePopGestureRecognizer?.delegate = null }
            navigationDelegates.forEach { it.navigation = null; it.onStackChanged = null }
        }
    }
    return true
}

private fun nativeRootTabIndices(isLoggedIn: Boolean, businessMode: Boolean): List<Int> =
    mainTabScreens(isLoggedIn, businessMode).map { requireNotNull(nativeTabIndex(it)) }
        .toMutableList().apply { add((size + 1) / 2, 5) }

private fun installRootTabsWithCreate(tabs: UITabBarController, navigations: List<UINavigationController>,
    createNavigation: UINavigationController, indices: List<Int>, restoredTab: Int, title: String) {
    createNavigation.tabBarItem = UITabBarItem(title = title, image = UIImage.systemImageNamed("plus"), tag = 5L)
    tabs.installRootTabs(indices.map { if (it == 5) createNavigation else navigations[it] },
        indices.indexOf(restoredTab).coerceAtLeast(0))
}

@Composable
private fun NativeCreateAction(tabDelegate: TabDelegate, refreshCommunity: (() -> Unit)?, visible: Boolean) {
    val sessions: SessionReader = koinInject()
    val session by remember(sessions) { sessions.observeSession() }.collectAsState(sessions.peekSession())
    TrackNativeCreateActionExposure(visible)
    CheckInCreateHost(
        canCreate = { sessions.isActiveSession(sessions.peekSession()) },
        resetKey = session?.userId ?: session?.accessToken,
        onShop = { Navigator.navigate(Navigator.Screen.CreateCheckIn) },
        onHomeSaved = { refreshCommunity?.invoke() },
    ) { onCreate -> SideEffect { tabDelegate.onCreateRequested = onCreate } }
}

@Composable
private fun NativeRootTabContent(index: Int, isLoggedIn: Boolean, visibility: NativeTabBarVisibility,
    reselections: MainTabReselections, onRefreshChange: ((() -> Unit)?) -> Unit) {
    when (index) {
        0 -> koinInject<DiscoveryScreenRenderer>().Tab(
            onMapExpandedChange = visibility::onMapExpandedChange, reselections = reselections.search)
        1 -> koinInject<CommunityScreenRenderer>().Content(
            notificationsAction = { koinInject<com.coffeepeek.admin.ui.screen.notifications.NotificationsRenderer>().Bell() },
            onRefreshChange = onRefreshChange)
        2 -> koinInject<OwnProfileScreenRenderer>().Content(isLoggedIn)
        3 -> koinInject<SettingsScreenRenderer>().Tab()
        4 -> koinInject<com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry>().Tab()
    }
}

private fun updateNativeTabLabels(navigations: List<UINavigationController>, labels: List<String>) {
    navigations.forEachIndexed { index, navigation ->
        navigation.tabBarItem.title = labels[index]
        (navigation.viewControllers.firstOrNull() as? UIViewController)?.title = labels[index]
    }
}
