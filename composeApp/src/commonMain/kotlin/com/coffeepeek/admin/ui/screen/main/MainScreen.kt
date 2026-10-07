package com.coffeepeek.admin.ui.screen.main

import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.Navigator.isHandledByRootNav
import com.coffeepeek.admin.ui.component.PlatformFloatingBottomNavBar
import com.coffeepeek.admin.ui.component.floatingNavBarHeight
import com.coffeepeek.admin.ui.component.FloatingNavBottomMargin
import com.coffeepeek.admin.ui.component.FloatingNavItem
import com.coffeepeek.admin.ui.component.RetainedContent
import com.coffeepeek.admin.ui.component.ProvideFloatingNavClearance
import com.coffeepeek.admin.ui.screen.feed.FeedScreen
import com.coffeepeek.admin.feature.coffee.ui.CoffeeListScreen
import com.coffeepeek.admin.ui.screen.feed.FeedViewModel
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.ui.screen.map.MapScreen
import com.coffeepeek.admin.ui.screen.roaster.RoasterPreview
import com.coffeepeek.admin.ui.screen.roaster.RoasterListViewModel
import com.coffeepeek.admin.ui.screen.profile.ProfileScreen
import com.coffeepeek.admin.ui.screen.profile.SettingsScreen
import com.coffeepeek.admin.ui.icons.CpIcons

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val graph: Navigator.Screen,
    val startScreen: Navigator.Screen,
)

@Composable
expect fun MainScreen()

@Composable
internal fun ComposeMainScreen() {
    val bottomNavController = rememberNavController()
    var isFeedMapExpanded by remember { mutableStateOf(false) }
    val pendingTabSelection by Navigator.pendingTabSelection.collectAsState()

    LaunchedEffect(Unit) {
        Navigator.navigationEvents.collect { event ->
            when (event) {
                is Navigator.NavEvent.SelectTab -> {
                    bottomNavController.navigate(event.tab) {
                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                is Navigator.NavEvent.NavigateTo -> {
                    if (!event.screen.isHandledByRootNav()) {
                        bottomNavController.navigate(event.screen)
                    }
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(pendingTabSelection) {
        pendingTabSelection?.let { tab ->
            bottomNavController.navigate(tab) {
                popUpTo(bottomNavController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            Navigator.consumeTabSelection()
        }
    }

    val items = listOf(
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
    )

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isExpandedMapVisible = isFeedMapExpanded &&
        currentDestination?.hasRoute<Navigator.Screen.FeedTab>() == true
    val density = LocalDensity.current
    val systemNavBottom = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val floatingClearance = systemNavBottom + floatingNavBarHeight() + FloatingNavBottomMargin
    val tabBarHaze = rememberHazeState()
    var feedEntry by remember { mutableStateOf<NavBackStackEntry?>(null) }
    val feedStateHolder = rememberSaveableStateHolder()

    ProvideFloatingNavClearance(clearance = floatingClearance) {
        Box(modifier = Modifier.fillMaxSize()) {
            feedEntry?.takeIf { it.lifecycle.currentState != Lifecycle.State.DESTROYED }?.let { entry ->
                val lifecycleState by entry.lifecycle.currentStateFlow.collectAsState()
                entry.LocalOwnersProvider(feedStateHolder) {
                    RetainedContent(visible = lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
                        var showRoasters by rememberSaveable { mutableStateOf(false) }
                        val feedVm: FeedViewModel = platformViewModel()
                        val feedState by feedVm.uiState.collectAsState()
                        val roasterVm: RoasterListViewModel = platformViewModel()
                        val pendingMapFocus by Navigator.pendingMapFocus.collectAsState()
                        LaunchedEffect(pendingMapFocus) {
                            if (pendingMapFocus != null) showRoasters = false
                        }
                        val searchOpacity = animateFloatAsState(if (showRoasters) 0f else 1f, tween(360), label = "discovery-roaster-list")
                        val searchVisible by remember { derivedStateOf { searchOpacity.value > 0f } }
                        Box(Modifier.fillMaxSize().hazeSource(tabBarHaze)) {
                            RetainedContent(visible = searchVisible) {
                                Box(Modifier.fillMaxSize().graphicsLayer {
                                    alpha = searchOpacity.value
                                    translationY = -size.height / 5f * (1f - searchOpacity.value)
                                }) {
                                    FeedScreen(
                                        vm = feedVm,
                                        onSelectRoasters = { showRoasters = true },
                                        mapPreview = { expanded, onToggleExpand, canvasSize, modifier ->
                                            MapScreen(
                                                modifier = modifier,
                                                isPreview = !expanded,
                                                onToggleExpand = onToggleExpand,
                                                canvasSize = canvasSize,
                                            )
                                        },
                                        roasterPreview = {
                                            RoasterPreview(
                                                query = feedState.query,
                                                selectedRoasterIds = feedState.filters.roasterIds,
                                                favoritesOnly = feedState.filters.favoritesOnly,
                                                vm = roasterVm,
                                            )
                                        },
                                        onMapExpandedChange = { isFeedMapExpanded = it },
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = showRoasters,
                                modifier = Modifier.fillMaxSize(),
                                enter = fadeIn(tween(300)) + slideInVertically(tween(360)) { it / 5 },
                                exit = fadeOut(tween(240)) + slideOutVertically(tween(360)) { -it / 5 },
                            ) {
                                com.coffeepeek.admin.ui.screen.roaster.RoasterListScreen(
                                    onCancel = {
                                        feedVm.cancelSearch()
                                        showRoasters = false
                                    },
                                    query = feedState.query,
                                    onQueryChange = feedVm::onQueryChange,
                                    selectedRoasterIds = feedState.filters.roasterIds,
                                    favoritesOnly = feedState.filters.favoritesOnly,
                                    vm = roasterVm,
                                )
                            }
                        }
                    }
                }
            }
            NavHost(
                navController = bottomNavController,
                startDestination = Navigator.Screen.FeedGraph,
                // Tab content scrolls under the glass tab bar and is blurred by it.
                modifier = Modifier.fillMaxSize().hazeSource(tabBarHaze),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                navigation<Navigator.Screen.FeedGraph>(startDestination = Navigator.Screen.FeedTab) {
                    composable<Navigator.Screen.FeedTab> {
                        SideEffect { feedEntry = it }
                    }
                }

                navigation<Navigator.Screen.CoffeeGraph>(startDestination = Navigator.Screen.CoffeeTab) {
                    composable<Navigator.Screen.CoffeeTab> { CoffeeListScreen() }
                }

                navigation<Navigator.Screen.CommunityGraph>(startDestination = Navigator.Screen.CommunityTab) {
                    composable<Navigator.Screen.CommunityTab> { com.coffeepeek.admin.feature.community.ui.CommunityScreen() }
                }

                navigation<Navigator.Screen.ProfileGraph>(startDestination = Navigator.Screen.ProfileTab) {
                    composable<Navigator.Screen.ProfileTab> { ProfileScreen() }
                }

                navigation<Navigator.Screen.SettingsGraph>(startDestination = Navigator.Screen.SettingsTab) {
                    composable<Navigator.Screen.SettingsTab> { SettingsScreen() }
                }
            }

            AnimatedVisibility(
                visible = !isExpandedMapVisible,
                enter = fadeIn(tween(250)) + slideInVertically(tween(360)) { it },
                exit = fadeOut(tween(250)) + slideOutVertically(tween(360)) { it },
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
