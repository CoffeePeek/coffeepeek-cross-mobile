package com.coffeepeek.admin.ui

import com.coffeepeek.admin.ui.screen.addshop.AddShopScreen
import com.coffeepeek.admin.ui.screen.auth.registr.RegisterScreen
import com.coffeepeek.admin.ui.screen.checkins.VisitedPlacesScreen
import com.coffeepeek.admin.ui.screen.deleteaccount.DeleteAccountPendingScreen
import com.coffeepeek.admin.ui.screen.editprofile.EditProfileScreen
import com.coffeepeek.admin.ui.screen.favorites.FavoritesScreen
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.admin.ui.screen.contributions.MyContributionsScreen
import com.coffeepeek.admin.ui.screen.roaster.AddRoasterScreen
import com.coffeepeek.admin.ui.screen.roaster.RoasterDetailScreen
import com.coffeepeek.admin.ui.screen.profile.CityScreen
import com.coffeepeek.admin.ui.screen.profile.ThemeScreen
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.LocalOwnersProvider
import com.coffeepeek.admin.di.platformViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.coffeepeek.admin.ui.screen.shop.CheckInDraftStore
import org.koin.compose.koinInject
import com.coffeepeek.admin.ui.dialogs.ErrorDialog
import com.coffeepeek.admin.ui.dialogs.LoadingDialog
import com.coffeepeek.admin.ui.screen.auth.AuthScreen
import com.coffeepeek.admin.ui.screen.main.MainScreen
import com.coffeepeek.admin.ui.component.RetainedContent
import com.coffeepeek.admin.ui.screen.shop.ShopDetailScreen
import com.coffeepeek.admin.feature.coffee.ui.CoffeeDetailScreen
import com.coffeepeek.admin.ui.screen.shop.ShopMenuGalleryScreen
import com.coffeepeek.admin.ui.screen.shop.ShopReportScreen
import com.coffeepeek.admin.ui.screen.shopchange.ShopChangeEditorScreen
import com.coffeepeek.admin.ui.screen.shopchange.ShopChangeRequestDetailScreen
import com.coffeepeek.admin.ui.screen.shopchange.SuggestShopChangeScreen
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.admin.utils.LoadingHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable

private const val ROOT_NAV_ANIMATION_DURATION_MS = 300

object Navigator {

    private val navigatorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _navigationEvents = MutableSharedFlow<NavEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed interface NavEvent {
        data class NavigateTo(val screen: Screen) : NavEvent
        data class SelectTab(val tab: Screen) : NavEvent
        data object PopBack : NavEvent
        data object NavigateUp : NavEvent
    }

    @Serializable
    sealed interface Screen {
        // Root screens (outside bottom nav)
        @Serializable data object Auth : Screen
        @Serializable data object Register : Screen
        @Serializable data object Main : Screen

        // Graphs
        @Serializable data object FeedGraph : Screen
        @Serializable data object CoffeeGraph : Screen
        @Serializable data object CommunityGraph : Screen
        @Serializable data object ProfileGraph : Screen
        @Serializable data object SettingsGraph : Screen

        // Tabs
        @Serializable data object FeedTab : Screen
        @Serializable data object CoffeeTab : Screen
        @Serializable data object CommunityTab : Screen
        @Serializable data object ProfileTab : Screen
        @Serializable data object SettingsTab : Screen

        // Inner screens (add here + in the graph in MainScreen)
        @Serializable data class ShopDetail(val shopId: String) : Screen
        @Serializable data class CoffeeDetail(val slug: String) : Screen
        @Serializable data class ShopMenuGallery(val shopId: String) : Screen
        @Serializable data class ReportShop(val shopId: String, val shopTitle: String) : Screen
        @Serializable data class SuggestShopChange(val shopId: String) : Screen
        @Serializable data class ShopChangeEditor(
            val shopId: String,
            val section: String,
            val requestId: String = "",
        ) : Screen
        /** [kind] is a [ContributionKind] name. */
        @Serializable data class MyContributions(val kind: String) : Screen
        @Serializable data class ShopChangeRequestDetail(
            val requestId: String,
        ) : Screen
        @Serializable data object AddShop : Screen
        @Serializable data object AddRoaster : Screen
        @Serializable data class RoasterDetail(val roasterId: String) : Screen
        @Serializable data object EditProfile : Screen
        @Serializable data object DeleteAccountPending : Screen
        @Serializable data object Favorites : Screen
        @Serializable data object VisitedPlaces : Screen
        @Serializable data object CitySettings : Screen
        @Serializable data object ThemeSettings : Screen
        @Serializable data class ShopReviews(val shopId: String) : Screen
        @Serializable data class ReportReview(val reviewId: String, val isPreview: Boolean = false) : Screen
    }

    data class MapShopFocus(
        val shopId: String,
        val latitude: Double,
        val longitude: Double,
        val title: String,
    )

    private val _pendingMapFocus = MutableStateFlow<MapShopFocus?>(null)
    val pendingMapFocus = _pendingMapFocus.asStateFlow()

    private val _pendingTabSelection = MutableStateFlow<Screen?>(null)
    val pendingTabSelection = _pendingTabSelection.asStateFlow()

    private val _openLoginAfterSessionEnd = MutableStateFlow(false)
    private val pendingAppLink = MutableStateFlow<Screen?>(null)

    internal fun openAppLink(screen: Screen) {
        pendingAppLink.value = screen
    }

    fun consumeMapFocus() {
        _pendingMapFocus.value = null
    }

    fun consumeTabSelection() {
        _pendingTabSelection.value = null
    }

    fun openLoginAfterSessionEnd() {
        _openLoginAfterSessionEnd.value = true
    }

    fun openShopOnMap(shopId: String, latitude: Double, longitude: Double, title: String) {
        navigatorScope.launch {
            _pendingMapFocus.value = MapShopFocus(shopId, latitude, longitude, title)
            _navigationEvents.emit(NavEvent.PopBack)
            _pendingTabSelection.value = Screen.FeedTab
            _navigationEvents.emit(NavEvent.SelectTab(Screen.FeedTab))
        }
    }

    fun Screen.isHandledByRootNav(): Boolean = when (this) {
        is Screen.Auth,
        is Screen.Register,
        is Screen.Main,
        is Screen.ShopDetail,
        is Screen.CoffeeDetail,
        is Screen.ShopMenuGallery,
        is Screen.ReportShop,
        is Screen.SuggestShopChange,
        is Screen.ShopChangeEditor,
        is Screen.MyContributions,
        is Screen.ShopChangeRequestDetail,
        is Screen.AddShop,
        is Screen.AddRoaster,
        is Screen.RoasterDetail,
        is Screen.EditProfile,
        is Screen.DeleteAccountPending,
        is Screen.ShopReviews,
        is Screen.ReportReview,
        is Screen.Favorites,
        is Screen.VisitedPlaces,
        is Screen.CitySettings,
        is Screen.ThemeSettings -> true
        else -> false
    }

    fun navigate(screen: Screen) {
        navigatorScope.launch { _navigationEvents.emit(NavEvent.NavigateTo(screen)) }
    }

    fun selectTab(tab: Screen) {
        navigatorScope.launch {
            _pendingTabSelection.value = tab
            _navigationEvents.emit(NavEvent.SelectTab(tab))
        }
    }

    fun popBack() {
        navigatorScope.launch { _navigationEvents.emit(NavEvent.PopBack) }
    }

    fun closeAuth() {
        popBack()
    }

    /** Pop current screen, then navigate (ordered in one coroutine). */
    fun popThenNavigate(screen: Screen) {
        navigatorScope.launch {
            _navigationEvents.emit(NavEvent.PopBack)
            _navigationEvents.emit(NavEvent.NavigateTo(screen))
        }
    }

    @Composable
    operator fun invoke(
        vm: NavigatorViewModel = platformViewModel(),
    ) {
        val errorMessage = ErrorHandler.errorMessage.collectAsState().value
        val loading = LoadingHandler.isLoading.collectAsState().value
        val isLoggedIn by vm.isLoggedIn.collectAsState()
        val checkInDrafts: CheckInDraftStore = koinInject()

        ErrorDialog(
            show = errorMessage != null,
            message = errorMessage ?: "",
            onDismiss = { ErrorHandler.clearError() }
        )
        LaunchedEffect(isLoggedIn) {
            if (!isLoggedIn) {
                checkInDrafts.clearAll()
                ErrorHandler.clearError()
                LoadingHandler.clearLoading()
                if (_openLoginAfterSessionEnd.value) {
                    _openLoginAfterSessionEnd.value = false
                    yield()
                    navigate(Screen.Auth)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            key(isLoggedIn) {
                BaseNavigator()
            }
            LoadingDialog(show = loading)
        }
    }

    @Composable
    private fun BaseNavigator() {
        val nav = rememberNavController()
        val appLink by pendingAppLink.collectAsState()
        var mainEntry by remember { mutableStateOf<NavBackStackEntry?>(null) }
        val mainStateHolder = rememberSaveableStateHolder()

        LaunchedEffect(appLink) {
            val screen = appLink ?: return@LaunchedEffect
            nav.navigate(screen) {
                launchSingleTop = true
                popUpTo<Screen.Main>()
            }
            pendingAppLink.compareAndSet(screen, null)
        }

        LaunchedEffect(Unit) {
            navigationEvents.onEach { event ->
                when (event) {
                    is NavEvent.PopBack -> nav.popBackStack()
                    is NavEvent.NavigateUp -> nav.navigateUp()
                    is NavEvent.NavigateTo -> {
                        if (event.screen.isHandledByRootNav()) {
                            nav.navigate(event.screen)
                        }
                    }
                    else -> Unit
                }
            }.launchIn(this)
        }

        Box(Modifier.fillMaxSize()) {
            mainEntry?.takeIf { it.lifecycle.currentState != Lifecycle.State.DESTROYED }?.let { entry ->
                val lifecycleState by entry.lifecycle.currentStateFlow.collectAsState()
                entry.LocalOwnersProvider(mainStateHolder) {
                    RetainedContent(visible = lifecycleState.isAtLeast(Lifecycle.State.STARTED)) { MainScreen() }
                }
            }
            NavHost(
                navController = nav,
                startDestination = Screen.Main,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(
                            durationMillis = ROOT_NAV_ANIMATION_DURATION_MS,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(
                            durationMillis = ROOT_NAV_ANIMATION_DURATION_MS,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(
                            durationMillis = ROOT_NAV_ANIMATION_DURATION_MS,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(
                            durationMillis = ROOT_NAV_ANIMATION_DURATION_MS,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                },
            ) {
                    composable<Screen.Auth> { AuthScreen() }
                    composable<Screen.Register> { RegisterScreen() }
                    composable<Screen.Main> { entry ->
                        SideEffect { mainEntry = entry }
                    }
                    composable<Screen.ShopDetail> { backStack ->
                        val route = backStack.toRoute<Screen.ShopDetail>()
                        ShopDetailScreen(shopId = route.shopId)
                    }
                    composable<Screen.CoffeeDetail> { backStack ->
                        val route = backStack.toRoute<Screen.CoffeeDetail>()
                        CoffeeDetailScreen(slug = route.slug)
                    }
                    composable<Screen.ShopReviews> { backStack ->
                        val route = backStack.toRoute<Screen.ShopReviews>()
                        com.coffeepeek.admin.ui.screen.shop.ShopReviewsScreen(shopId = route.shopId)
                    }
                    composable<Screen.ReportReview> { backStack ->
                        val route = backStack.toRoute<Screen.ReportReview>()
                        com.coffeepeek.admin.ui.screen.review.ReviewReportScreen(reviewId = route.reviewId, isPreview = route.isPreview)
                    }
                    composable<Screen.ShopMenuGallery> { backStack ->
                        val route = backStack.toRoute<Screen.ShopMenuGallery>()
                        ShopMenuGalleryScreen(shopId = route.shopId)
                    }
                    composable<Screen.ReportShop> { backStack ->
                        val route = backStack.toRoute<Screen.ReportShop>()
                        ShopReportScreen(shopId = route.shopId, shopTitle = route.shopTitle)
                    }
                    composable<Screen.SuggestShopChange> { backStack ->
                        val route = backStack.toRoute<Screen.SuggestShopChange>()
                        SuggestShopChangeScreen(shopId = route.shopId)
                    }
                    composable<Screen.ShopChangeEditor> { backStack ->
                        val route = backStack.toRoute<Screen.ShopChangeEditor>()
                        ShopChangeEditorScreen(
                            shopId = route.shopId,
                            sectionName = route.section,
                            requestId = route.requestId,
                        )
                    }
                    composable<Screen.MyContributions> { backStack ->
                        val route = backStack.toRoute<Screen.MyContributions>()
                        MyContributionsScreen(kind = ContributionKind.valueOf(route.kind))
                    }
                    composable<Screen.ShopChangeRequestDetail> { backStack ->
                        val route = backStack.toRoute<Screen.ShopChangeRequestDetail>()
                        ShopChangeRequestDetailScreen(requestId = route.requestId)
                    }
                    composable<Screen.AddShop> { AddShopScreen() }
                    composable<Screen.AddRoaster> { AddRoasterScreen() }
                    composable<Screen.RoasterDetail> { backStack ->
                        val route = backStack.toRoute<Screen.RoasterDetail>()
                        RoasterDetailScreen(roasterId = route.roasterId)
                    }
                    composable<Screen.EditProfile> { EditProfileScreen() }
                    composable<Screen.DeleteAccountPending> { DeleteAccountPendingScreen() }
                    composable<Screen.Favorites> { FavoritesScreen() }
                    composable<Screen.VisitedPlaces> { VisitedPlacesScreen() }
                    composable<Screen.CitySettings> { CityScreen() }
                    composable<Screen.ThemeSettings> { ThemeScreen() }
            }
        }
    }
}
