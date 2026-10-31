package com.coffeepeek.admin.ui.screen.main

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuBackdrop
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavBackStackEntry
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.MainTabReselections
import com.coffeepeek.admin.ui.Navigator.isHandledByRootNav
import com.coffeepeek.admin.ui.mainTabScreens
import com.coffeepeek.admin.ui.component.PlatformFloatingBottomNavBar
import com.coffeepeek.admin.ui.component.floatingNavBarHeight
import com.coffeepeek.admin.ui.component.FloatingNavBottomMargin
import com.coffeepeek.admin.ui.component.FloatingNavItem
import com.coffeepeek.admin.ui.component.RetainedContent
import com.coffeepeek.admin.ui.component.ProvideFloatingNavClearance
import com.coffeepeek.admin.ui.screen.coffee.CoffeeScreenRenderer
import org.koin.compose.koinInject
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.business_tab_title
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.admin.ui.screen.profile.OwnProfileScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.SettingsScreenRenderer
import com.coffeepeek.admin.ui.icons.CpIcons

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val graph: Navigator.Screen,
    val startScreen: Navigator.Screen,
)

@Composable
expect fun MainScreen(isLoggedIn: Boolean, onMapExpandedChange: (Boolean) -> Unit = {})

@Composable
internal fun ComposeMainScreen(isLoggedIn: Boolean, onMapExpandedChange: (Boolean) -> Unit) {
    val reduceMotion = com.coffeepeek.admin.ui.component.platformReduceMotion()
    val bottomNavController = rememberNavController()
    val tabReselections = remember { MainTabReselections() }
    val analyticsEntry by bottomNavController.currentBackStackEntryAsState()
    com.coffeepeek.admin.analytics.TrackNavigationScreen(analyticsEntry)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var isFeedMapExpanded by remember { mutableStateOf(false) }
    val pendingTabSelection by Navigator.pendingTabSelection.collectAsState()

    val businessMode = koinInject<com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry>().Session().businessMode
    val availableTabs = mainTabScreens(isLoggedIn, businessMode)
    val items = listOf(
        BottomNavItem(title = stringResource(Res.string.business_tab_title), icon = CpIcons.CoffeeBean, graph = Navigator.Screen.BusinessGraph, startScreen = Navigator.Screen.BusinessTab),
        BottomNavItem(
            title = "Поиск",
            icon = CpIcons.Search,
            graph = Navigator.Screen.FeedGraph,
            startScreen = Navigator.Screen.FeedTab,
        ),
        BottomNavItem(
            title = "Кофе",
            icon = CpIcons.CoffeeBean,
            graph = Navigator.Screen.CoffeeGraph,
            startScreen = Navigator.Screen.CoffeeTab,
        ),
        BottomNavItem(
            title = "Лента",
            icon = CpIcons.Community,
            graph = Navigator.Screen.CommunityGraph,
            startScreen = Navigator.Screen.CommunityTab,
        ),
        BottomNavItem(
            title = "Профиль",
            icon = CpIcons.Profile,
            graph = Navigator.Screen.ProfileGraph,
            startScreen = Navigator.Screen.ProfileTab,
        ),
        BottomNavItem(
            title = "Настройки",
            icon = CpIcons.Settings,
            graph = Navigator.Screen.SettingsGraph,
            startScreen = Navigator.Screen.SettingsTab,
        ),
    ).filter { it.startScreen in availableTabs }
    val availableRoutes = items.flatMap { listOf(it.graph, it.startScreen) }

    var previousBusinessMode by remember { mutableStateOf(businessMode) }
    LaunchedEffect(businessMode) {
        if (previousBusinessMode == businessMode) return@LaunchedEffect
        previousBusinessMode = businessMode
        bottomNavController.navigate(if (businessMode) Navigator.Screen.BusinessGraph else Navigator.Screen.FeedGraph) {
            popUpTo(bottomNavController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
    LaunchedEffect(availableRoutes) {
        Navigator.navigationEvents.collect { event ->
            when (event) {
                is Navigator.NavEvent.SelectTab -> {
                    if (event.tab !in availableRoutes) return@collect
                    bottomNavController.navigate(event.tab) {
                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                is Navigator.NavEvent.NavigateTo -> {
                    if (!event.screen.isHandledByRootNav() && event.screen in availableRoutes) {
                        bottomNavController.navigate(event.screen)
                    }
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(pendingTabSelection, availableRoutes) {
        pendingTabSelection?.let { tab ->
            if (tab in availableRoutes) {
                bottomNavController.navigate(tab) {
                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            Navigator.consumeTabSelection()
        }
    }

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    LaunchedEffect(currentDestination, isLoggedIn) {
        if (!isLoggedIn && currentDestination?.hasRoute<Navigator.Screen.ProfileTab>() == true) {
            bottomNavController.navigate(Navigator.Screen.FeedGraph) {
                popUpTo(bottomNavController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }
    val visibleEntries by bottomNavController.visibleEntries.collectAsState()
    val feedSlide by animateFloatAsState(
        targetValue = if (currentDestination == null || currentDestination.hasRoute<Navigator.Screen.FeedTab>()) 0f else -1f,
        animationSpec = tween(if (reduceMotion) 0 else 300, easing = FastOutSlowInEasing),
        label = "retained-feed-slide",
    )
    LaunchedEffect(navBackStackEntry) {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }
    val isExpandedMapVisible = isFeedMapExpanded &&
        currentDestination?.hasRoute<Navigator.Screen.FeedTab>() == true
    LaunchedEffect(isExpandedMapVisible) { onMapExpandedChange(isExpandedMapVisible) }
    val systemBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val floatingClearance = systemBottomInset +
        if (isExpandedMapVisible) 0.dp else floatingNavBarHeight() + FloatingNavBottomMargin
    val tabBarHaze = rememberHazeState()
    var feedEntry by remember { mutableStateOf<NavBackStackEntry?>(null) }
    val feedStateHolder = rememberSaveableStateHolder()

    ProvideFloatingNavClearance(clearance = floatingClearance) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Capture the visible tab once; retained search content must not keep a separate glass layer.
            CoffeePeekMenuBackdrop(Modifier.fillMaxSize(), state = tabBarHaze) {
                feedEntry?.takeIf { it.lifecycle.currentState != Lifecycle.State.DESTROYED }?.let { entry ->
                    entry.LocalOwnersProvider(feedStateHolder) {
                        RetainedContent(visible = entry in visibleEntries) {
                            Box(Modifier.fillMaxSize().graphicsLayer { translationX = size.width * feedSlide }) {
                                koinInject<DiscoveryScreenRenderer>().Tab(
                                    onMapExpandedChange = { isFeedMapExpanded = it }, reselections = tabReselections.search)
                            }
                        }
                    }
                }
                NavHost(
                    navController = bottomNavController,
                    startDestination = if (businessMode) Navigator.Screen.BusinessGraph else Navigator.Screen.FeedGraph,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        slideIntoContainer(tabDirection(), tween(if (reduceMotion) 0 else 300, easing = FastOutSlowInEasing))
                    },
                    exitTransition = {
                        slideOutOfContainer(tabDirection(), tween(if (reduceMotion) 0 else 300, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        slideIntoContainer(tabDirection(), tween(if (reduceMotion) 0 else 300, easing = FastOutSlowInEasing))
                    },
                    popExitTransition = {
                        slideOutOfContainer(tabDirection(), tween(if (reduceMotion) 0 else 300, easing = FastOutSlowInEasing))
                    },
                ) {
                    navigation<Navigator.Screen.FeedGraph>(startDestination = Navigator.Screen.FeedTab) {
                        composable<Navigator.Screen.FeedTab> {
                            Box(Modifier.fillMaxSize())
                            SideEffect { feedEntry = it }
                        }
                    }

                    navigation<Navigator.Screen.CoffeeGraph>(startDestination = Navigator.Screen.CoffeeTab) {
                        composable<Navigator.Screen.CoffeeTab> {
                            CheckInTabContent { koinInject<CoffeeScreenRenderer>().List(tabReselections.coffee) }
                        }
                    }

                    navigation<Navigator.Screen.CommunityGraph>(startDestination = Navigator.Screen.CommunityTab) {
                        composable<Navigator.Screen.CommunityTab> {
                            koinInject<com.coffeepeek.admin.feature.community.ui.CommunityScreenRenderer>().Content(
                                onCreateCheckIn = { Navigator.navigate(Navigator.Screen.CreateCheckIn) },
                                notificationsAction = { koinInject<com.coffeepeek.admin.feature.notifications.ui.NotificationsRenderer>().Bell() },
                            )
                        }
                    }

                    navigation<Navigator.Screen.ProfileGraph>(startDestination = Navigator.Screen.ProfileTab) {
                        composable<Navigator.Screen.ProfileTab> { if (isLoggedIn) koinInject<OwnProfileScreenRenderer>().Content() }
                    }

                    navigation<Navigator.Screen.BusinessGraph>(startDestination = Navigator.Screen.BusinessTab) {
                        composable<Navigator.Screen.BusinessTab> {
                            koinInject<com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry>().Tab()
                        }
                    }
                    navigation<Navigator.Screen.SettingsGraph>(startDestination = Navigator.Screen.SettingsTab) {
                        composable<Navigator.Screen.SettingsTab> { koinInject<SettingsScreenRenderer>().Tab() }
                    }
                }
            }

            AnimatedVisibility(
                visible = !isExpandedMapVisible,
                enter = fadeIn(tween(if (reduceMotion) 0 else 250)) + slideInVertically(tween(if (reduceMotion) 0 else 360)) { it },
                exit = fadeOut(tween(if (reduceMotion) 0 else 250)) + slideOutVertically(tween(if (reduceMotion) 0 else 360)) { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                PlatformFloatingBottomNavBar(
                    items = items.map { item ->
                        val isSelected = currentDestination?.hierarchy?.any { destination ->
                            destination.hasRoute(item.graph::class)
                        } == true
                        FloatingNavItem(
                            title = item.title,
                            icon = item.icon,
                            selected = isSelected,
                            onClick = {
                                tabReselections.onTabPressed(item.startScreen, isSelected)
                                if (isSelected) return@FloatingNavItem
                                bottomNavController.navigate(item.graph) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    },
                    // Android Compose glass uses a translucent tint over the native map.
                    hazeState = tabBarHaze.takeUnless { isExpandedMapVisible },
                )
            }
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection() =
    if (targetState.destination.tabIndex() >= initialState.destination.tabIndex()) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }

private fun NavDestination.tabIndex(): Int = when {
    hasRoute<Navigator.Screen.FeedTab>() -> 0
    hasRoute<Navigator.Screen.CoffeeTab>() -> 1
    hasRoute<Navigator.Screen.CommunityTab>() -> 2
    hasRoute<Navigator.Screen.ProfileTab>() -> 3
    else -> 4
}
