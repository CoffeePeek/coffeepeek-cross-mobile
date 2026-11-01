package com.coffeepeek.admin.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.screen.checkins.CheckInHistoryDestination
import com.coffeepeek.admin.ui.screen.shelf.ShelfScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopDetailScreen
import com.coffeepeek.admin.ui.screen.shop.ShopMenuGalleryScreen
import com.coffeepeek.admin.ui.screen.shop.ShopReportScreen
import com.coffeepeek.admin.ui.screen.favorites.FavoritesScreen
import com.coffeepeek.admin.ui.favorites.FavoritesDestination
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.admin.ui.screen.contributions.ContributionsScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.AccountScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.PublicProfileScreenRenderer
import com.coffeepeek.admin.ui.screen.roaster.RoasterScreenRenderer
import com.coffeepeek.admin.feature.community.ui.CheckInDetailScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.SettingsScreenRenderer
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.luminance
import com.coffeepeek.admin.theme.PlatformSystemBars
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.LocalOwnersProvider
import com.coffeepeek.admin.di.platformViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.coffeepeek.admin.ui.screen.shop.CheckInDraftStore
import org.koin.compose.koinInject
import com.coffeepeek.admin.ui.dialogs.ErrorDialog
import com.coffeepeek.admin.ui.dialogs.LoadingDialog
import com.coffeepeek.admin.ui.screen.auth.AuthScreenRenderer
import com.coffeepeek.admin.ui.screen.main.MainScreen
import com.coffeepeek.admin.ui.component.RetainedContent
import com.coffeepeek.admin.ui.screen.shop.ShopDetailScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopCheckInListScreenRenderer
import com.coffeepeek.admin.ui.screen.coffee.CoffeeScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopMenuGalleryScreenRenderer
import com.coffeepeek.admin.ui.screen.shop.ShopReportScreenRenderer
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
internal val RootMainSlideSpec = tween<Float>(
    durationMillis = ROOT_NAV_ANIMATION_DURATION_MS,
    easing = FastOutSlowInEasing,
)

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
        @Serializable data class ForgotPassword(val email: String = "") : Screen
        // Preserve the destination ID when Android restores a task from before web-only recovery.
        @Serializable data class ResetPassword(val token: String) : Screen
        @Serializable data object Main : Screen

        // Graphs
        @Serializable data object FeedGraph : Screen
        @Serializable data object CoffeeGraph : Screen
        @Serializable data object CommunityGraph : Screen
        @Serializable data object ProfileGraph : Screen
        @Serializable data object BusinessGraph : Screen
        @Serializable data object BusinessTab : Screen
        @Serializable data object SettingsGraph : Screen

        // Tabs
        @Serializable data object FeedTab : Screen
        @Serializable data object CoffeeTab : Screen
        @Serializable data object CommunityTab : Screen
        @Serializable data object ProfileTab : Screen
        @Serializable data object SettingsTab : Screen

        // Inner screens (add here + in the graph in MainScreen)
        @Serializable data class ShopDetail(val shopId: String, val forCheckIn: Boolean = false) : Screen
        @Serializable data object CreateCheckIn : Screen
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
        @Serializable data class MyBusiness(val kind: String? = null, val slug: String? = null, val businessId: String? = null) : Screen
        @Serializable data object AddShop : Screen
        @Serializable data object AddRoaster : Screen
        @Serializable data class RoasterDetail(val roasterId: String) : Screen
        @Serializable data class UserProfile(
            val slug: String,
            val nickname: String? = null,
            val avatarUrl: String? = null,
        ) : Screen
        @Serializable data class UserCheckIns(
            val slug: String,
            val nickname: String? = null,
            val avatarUrl: String? = null,
        ) : Screen
        @Serializable data object EditProfile : Screen
        @Serializable data object DeleteAccountPending : Screen
        @Serializable data object Favorites : Screen
        @Serializable data object VisitedPlaces : Screen
        @Serializable data object HomeCheckIns : Screen
        @Serializable data object ShelfArchive : Screen
        @Serializable data object ShelfAddBag : Screen
        @Serializable data class ShelfBag(val bagId: String) : Screen
        @Serializable data class EditHomeCheckIn(val id: String) : Screen
        @Serializable data object CitySettings : Screen
        @Serializable data object ThemeSettings : Screen
        @Serializable data object LanguageSettings : Screen
        @Serializable data object BlockedAuthors : Screen
        @Serializable data object UsagePolicy : Screen
        @Serializable data class ShopReviews(val shopId: String) : Screen
        @Serializable data class ReportReview(val reviewId: String, val isPreview: Boolean = false) : Screen
        @Serializable data class ReportCheckIn(val checkInId: String) : Screen
        /** A post opened from a shared link (coffeepeek.by/check-ins/{id}). */
        @Serializable data class CheckInDetail(val id: String) : Screen
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
        is Screen.ForgotPassword,
        is Screen.ResetPassword,
        is Screen.Main,
        is Screen.ShopDetail,
        is Screen.CreateCheckIn,
        is Screen.CoffeeDetail,
        is Screen.ShopMenuGallery,
        is Screen.ReportShop,
        is Screen.SuggestShopChange,
        is Screen.ShopChangeEditor,
        is Screen.MyContributions,
        is Screen.MyBusiness,
        is Screen.ShopChangeRequestDetail,
        is Screen.AddShop,
        is Screen.AddRoaster,
        is Screen.RoasterDetail,
        is Screen.UserProfile,
        is Screen.UserCheckIns,
        is Screen.EditProfile,
        is Screen.DeleteAccountPending,
        is Screen.ShopReviews,
        is Screen.ReportReview,
        is Screen.ReportCheckIn,
        is Screen.CheckInDetail,
        is Screen.Favorites,
        is Screen.VisitedPlaces,
        is Screen.HomeCheckIns,
        is Screen.ShelfArchive,
        is Screen.ShelfAddBag,
        is Screen.ShelfBag,
        is Screen.EditHomeCheckIn,
        is Screen.CitySettings,
        is Screen.ThemeSettings,
        is Screen.LanguageSettings,
        is Screen.BlockedAuthors,
        is Screen.UsagePolicy -> true
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

    fun popThenSelectTab(tab: Screen) {
        navigatorScope.launch {
            _navigationEvents.emit(NavEvent.PopBack)
            _pendingTabSelection.value = tab
            _navigationEvents.emit(NavEvent.SelectTab(tab))
        }
    }

    @Composable
    operator fun invoke(
        vm: NavigatorViewModel = platformViewModel(),
    ) {
        val errorMessage = ErrorHandler.errorMessage.collectAsState().value
        val loading = LoadingHandler.isLoading.collectAsState().value
        val isLoggedIn by vm.isLoggedIn.collectAsState()
        var previousLoginState by remember { mutableStateOf<Boolean?>(null) }
        var navigationGeneration by remember { mutableIntStateOf(0) }
        val checkInDrafts: CheckInDraftStore = koinInject()

        ErrorDialog(
            show = errorMessage != null,
            message = errorMessage ?: "",
            onDismiss = { ErrorHandler.clearError() }
        )
        LaunchedEffect(isLoggedIn) {
            val loggedIn = isLoggedIn ?: return@LaunchedEffect
            if (previousLoginState != null && previousLoginState != loggedIn) {
                clearPlatformNavigationState()
                navigationGeneration++
            }
            previousLoginState = loggedIn
            if (!loggedIn) {
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
            if (isLoggedIn != null) {
                key(navigationGeneration, isLoggedIn) {
                    BaseNavigator(isLoggedIn = isLoggedIn == true)
                }
            }
            LoadingDialog(show = loading)
        }
    }

    @Composable
    private fun BaseNavigator(isLoggedIn: Boolean) {
        koinInject<com.coffeepeek.admin.feature.notifications.ui.NotificationsRenderer>().HandlePendingOpen(isLoggedIn)
        val nativeAppLink by pendingAppLink.collectAsState()
        if (PlatformRootNavigation(isLoggedIn, navigationEvents, nativeAppLink,
            { pendingAppLink.compareAndSet(nativeAppLink, null) },
            { screen, login -> RootScreen(screen, login, isLoggedIn) },
        )) return

        val nav = rememberNavController()
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val appLink by pendingAppLink.collectAsState()
        var mainEntry by remember { mutableStateOf<NavBackStackEntry?>(null) }
        var isMainMapExpanded by remember { mutableStateOf(false) }
        val mainStateHolder = rememberSaveableStateHolder()
        val visibleEntries by nav.visibleEntries.collectAsState()
        val currentEntry by nav.currentBackStackEntryAsState()
        com.coffeepeek.admin.analytics.TrackNavigationScreen(currentEntry)
        LaunchedEffect(currentEntry) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
        val mainSlide by animateFloatAsState(
            targetValue = if (currentEntry?.destination?.hasRoute<Screen.Main>() != false) 0f else -1f,
            animationSpec = RootMainSlideSpec,
            label = "retained-main-slide",
        )

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
                            nav.navigateFromRoot(event.screen)
                        }
                    }
                    else -> Unit
                }
            }.launchIn(this)
        }

        val shopPhotoUnderStatusBar = currentEntry?.destination?.let {
            it.hasRoute<Screen.ShopDetail>() || it.hasRoute<Screen.RoasterDetail>()
        } == true
        val systemInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
        val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
        PlatformSystemBars(darkTheme, lightStatusBarIcons = shopPhotoUnderStatusBar || darkTheme)
        // Native iOS navigation returns above; this is the Compose navigation host.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clipToBounds()) {
            mainEntry?.takeIf { it.lifecycle.currentState != Lifecycle.State.DESTROYED }?.let { entry ->
                entry.LocalOwnersProvider(mainStateHolder) {
                    RetainedContent(visible = entry in visibleEntries) {
                        // Main lives outside NavHost so its native map stays mounted during navigation.
                        Box(Modifier.fillMaxSize()
                            .windowInsetsPadding(if (isMainMapExpanded) WindowInsets(0) else systemInsets.only(WindowInsetsSides.Top))
                            .clipToBounds().graphicsLayer { translationX = size.width * mainSlide }) {
                            MainScreen(isLoggedIn, onMapExpandedChange = { isMainMapExpanded = it })
                        }
                    }
                }
            }
            NavHost(
                navController = nav,
                startDestination = Screen.Main,
                modifier = Modifier.fillMaxSize().windowInsetsPadding(
                    if (shopPhotoUnderStatusBar) WindowInsets(0) else systemInsets.only(WindowInsetsSides.Top),
                ).clipToBounds(),
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
                    composable<Screen.Auth> { koinInject<AuthScreenRenderer>().Login() }
                    composable<Screen.Register> { koinInject<AuthScreenRenderer>().Register() }
                    composable<Screen.ForgotPassword> { backStack ->
                        koinInject<AuthScreenRenderer>().Recovery(
                            initialEmail = backStack.toRoute<Screen.ForgotPassword>().email,
                            onClose = { nav.popBackStack() },
                            onBackToLogin = {
                                nav.navigate(Screen.Auth) { popUpTo<Screen.Main>(); launchSingleTop = true }
                            },
                        )
                    }
                    composable<Screen.ResetPassword> {
                        LaunchedEffect(Unit) {
                            nav.navigate(Screen.Auth) { popUpTo<Screen.Main>(); launchSingleTop = true }
                        }
                    }
                    composable<Screen.Main> { entry ->
                        // The retained screen still needs a full-size transition target.
                        Box(Modifier.fillMaxSize())
                        SideEffect { mainEntry = entry }
                    }
                    composable<Screen.ShopDetail> { backStack ->
                        val route = backStack.toRoute<Screen.ShopDetail>()
                        koinInject<ShopDetailScreenRenderer>().Content(shopId = route.shopId, forCheckIn = route.forCheckIn)
                    }
                    composable<Screen.CreateCheckIn> {
                        com.coffeepeek.admin.ui.screen.shop.CheckInShopPickerScreen()
                    }
                    composable<Screen.CoffeeDetail> { backStack ->
                        val route = backStack.toRoute<Screen.CoffeeDetail>()
                        koinInject<CoffeeScreenRenderer>().Detail(route.slug)
                    }
                    composable<Screen.ShopReviews> { backStack ->
                        val route = backStack.toRoute<Screen.ShopReviews>()
                        koinInject<ShopCheckInListScreenRenderer>().Content(shopId = route.shopId)
                    }
                    composable<Screen.ReportReview> { backStack ->
                        val route = backStack.toRoute<Screen.ReportReview>()
                        com.coffeepeek.admin.ui.screen.review.ReviewReportScreen(reviewId = route.reviewId, isPreview = route.isPreview)
                    }
                    composable<Screen.ReportCheckIn> { backStack ->
                        val route = backStack.toRoute<Screen.ReportCheckIn>()
                        com.coffeepeek.admin.ui.screen.review.ReviewReportScreen(reviewId = route.checkInId, isCheckIn = true)
                    }
                    composable<Screen.CheckInDetail> { backStack ->
                        koinInject<CheckInDetailScreenRenderer>().Content(backStack.toRoute<Screen.CheckInDetail>().id)
                    }
                    composable<Screen.ShopMenuGallery> { backStack ->
                        val route = backStack.toRoute<Screen.ShopMenuGallery>()
                        koinInject<ShopMenuGalleryScreenRenderer>().Content(shopId = route.shopId)
                    }
                    composable<Screen.ReportShop> { backStack ->
                        val route = backStack.toRoute<Screen.ReportShop>()
                        koinInject<ShopReportScreenRenderer>().Content(route.shopId, route.shopTitle)
                    }
                    composable<Screen.SuggestShopChange> { backStack ->
                        val route = backStack.toRoute<Screen.SuggestShopChange>()
                        koinInject<ContributionsScreenRenderer>().Suggest(route.shopId)
                    }
                    composable<Screen.ShopChangeEditor> { backStack ->
                        val route = backStack.toRoute<Screen.ShopChangeEditor>()
koinInject<ContributionsScreenRenderer>().Editor(route.shopId, route.section, route.requestId)
                    }
                    composable<Screen.MyContributions> { backStack ->
                        val route = backStack.toRoute<Screen.MyContributions>()
                        koinInject<ContributionsScreenRenderer>().List(ContributionKind.valueOf(route.kind))
                    }
                    composable<Screen.ShopChangeRequestDetail> { backStack ->
                        val route = backStack.toRoute<Screen.ShopChangeRequestDetail>()
                        koinInject<ContributionsScreenRenderer>().RequestDetail(route.requestId)
                    }
                    composable<Screen.MyBusiness> { entry ->
                        BusinessOwnershipRoute(entry.toRoute<Screen.MyBusiness>())
                    }
                    composable<Screen.AddShop> { koinInject<ContributionsScreenRenderer>().AddShop() }
                    composable<Screen.AddRoaster> { koinInject<ContributionsScreenRenderer>().AddRoaster() }
                    composable<Screen.RoasterDetail> { backStack ->
                        val route = backStack.toRoute<Screen.RoasterDetail>()
                        koinInject<RoasterScreenRenderer>().Detail(route.roasterId)
                    }
                    composable<Screen.UserProfile> { backStack ->
                        PublicProfileRoute(backStack.toRoute<Screen.UserProfile>())
                    }
                    composable<Screen.UserCheckIns> { backStack ->
                        PublicCheckInsRoute(backStack.toRoute<Screen.UserCheckIns>())
                    }
                    composable<Screen.EditProfile> { koinInject<AccountScreenRenderer>().EditProfile() }
                    composable<Screen.DeleteAccountPending> { koinInject<AccountScreenRenderer>().DeletionPending() }
                    composable<Screen.Favorites> { FavoritesDestination() }
                    composable<Screen.VisitedPlaces> { CheckInHistoryDestination() }
                    composable<Screen.HomeCheckIns> { CheckInHistoryDestination() }
                    composable<Screen.ShelfArchive> { koinInject<ShelfScreenRenderer>().Archive() }
                    composable<Screen.ShelfAddBag> { koinInject<ShelfScreenRenderer>().AddBag() }
                    composable<Screen.ShelfBag> { backStack ->
                        val route = backStack.toRoute<Screen.ShelfBag>()
                        koinInject<ShelfScreenRenderer>().Bag(route.bagId)
                    }
                    composable<Screen.EditHomeCheckIn> { entry ->
                        CheckInHistoryDestination(initialHomeId = entry.toRoute<Screen.EditHomeCheckIn>().id)
                    }
                    composable<Screen.CitySettings> { koinInject<SettingsScreenRenderer>().City() }
                    composable<Screen.ThemeSettings> { koinInject<SettingsScreenRenderer>().Theme() }
                    composable<Screen.LanguageSettings> { koinInject<SettingsScreenRenderer>().Language() }
                    composable<Screen.BlockedAuthors> { com.coffeepeek.admin.feature.authorblocking.ui.BlockedAuthorsScreen() }
                    composable<Screen.UsagePolicy> { koinInject<SettingsScreenRenderer>().UsagePolicy() }
            }
        }
    }

    @Composable
    private fun RootScreen(screen: Screen, resetToLogin: () -> Unit, isLoggedIn: Boolean) {
        when (screen) {
            Screen.Main -> MainScreen(isLoggedIn)
            Screen.Auth -> koinInject<AuthScreenRenderer>().Login()
            Screen.Register -> koinInject<AuthScreenRenderer>().Register()
            is Screen.ForgotPassword -> koinInject<AuthScreenRenderer>().Recovery(initialEmail = screen.email, onClose = ::popBack, onBackToLogin = resetToLogin)
            is Screen.ResetPassword -> LaunchedEffect(Unit) { resetToLogin() }
            is Screen.ShopDetail -> koinInject<ShopDetailScreenRenderer>().Content(screen.shopId, screen.forCheckIn)
            Screen.CreateCheckIn -> com.coffeepeek.admin.ui.screen.shop.CheckInShopPickerScreen()
            is Screen.CoffeeDetail -> koinInject<CoffeeScreenRenderer>().Detail(screen.slug)
            is Screen.ShopReviews -> koinInject<ShopCheckInListScreenRenderer>().Content(screen.shopId)
            is Screen.ReportReview -> com.coffeepeek.admin.ui.screen.review.ReviewReportScreen(reviewId = screen.reviewId, isPreview = screen.isPreview)
            is Screen.ReportCheckIn -> com.coffeepeek.admin.ui.screen.review.ReviewReportScreen(reviewId = screen.checkInId, isCheckIn = true)
            is Screen.CheckInDetail -> koinInject<CheckInDetailScreenRenderer>().Content(screen.id)
            is Screen.ShopMenuGallery -> koinInject<ShopMenuGalleryScreenRenderer>().Content(screen.shopId)
            is Screen.ReportShop -> koinInject<ShopReportScreenRenderer>().Content(screen.shopId, screen.shopTitle)
            is Screen.SuggestShopChange -> koinInject<ContributionsScreenRenderer>().Suggest(screen.shopId)
            is Screen.ShopChangeEditor -> koinInject<ContributionsScreenRenderer>().Editor(screen.shopId, screen.section, screen.requestId)
            is Screen.MyContributions -> koinInject<ContributionsScreenRenderer>().List(ContributionKind.valueOf(screen.kind))
            is Screen.ShopChangeRequestDetail -> koinInject<ContributionsScreenRenderer>().RequestDetail(screen.requestId)
            is Screen.MyBusiness -> BusinessOwnershipRoute(screen)
            Screen.AddShop -> koinInject<ContributionsScreenRenderer>().AddShop()
            Screen.AddRoaster -> koinInject<ContributionsScreenRenderer>().AddRoaster()
            is Screen.RoasterDetail -> koinInject<RoasterScreenRenderer>().Detail(screen.roasterId)
            is Screen.UserProfile -> PublicProfileRoute(screen)
            is Screen.UserCheckIns -> PublicCheckInsRoute(screen)
            Screen.EditProfile -> koinInject<AccountScreenRenderer>().EditProfile()
            Screen.DeleteAccountPending -> koinInject<AccountScreenRenderer>().DeletionPending()
            Screen.Favorites -> FavoritesDestination()
            Screen.VisitedPlaces -> CheckInHistoryDestination()
            Screen.HomeCheckIns -> CheckInHistoryDestination()
            Screen.ShelfArchive -> koinInject<ShelfScreenRenderer>().Archive()
            Screen.ShelfAddBag -> koinInject<ShelfScreenRenderer>().AddBag()
            is Screen.ShelfBag -> koinInject<ShelfScreenRenderer>().Bag(screen.bagId)
            is Screen.EditHomeCheckIn -> CheckInHistoryDestination(initialHomeId = screen.id)
            Screen.CitySettings -> koinInject<SettingsScreenRenderer>().City()
            Screen.ThemeSettings -> koinInject<SettingsScreenRenderer>().Theme()
            Screen.LanguageSettings -> koinInject<SettingsScreenRenderer>().Language()
            Screen.BlockedAuthors -> com.coffeepeek.admin.feature.authorblocking.ui.BlockedAuthorsScreen()
            Screen.UsagePolicy -> koinInject<SettingsScreenRenderer>().UsagePolicy()
            else -> error("Tab route must be handled by MainScreen: $screen")
        }
    }

    private fun NavHostController.navigateFromRoot(screen: Screen) {
        val stack = currentBackStack.value.map { entry ->
            if (entry.destination.hasRoute<Screen.UserProfile>()) entry.toRoute<Screen.UserProfile>() else null
        }
        val existing = stack.getOrNull(existingProfileIndex(stack, screen))
        if (existing != null) popBackStack(existing, inclusive = false) else navigate(screen)
    }

    @Composable
    private fun PublicCheckInsRoute(screen: Screen.UserCheckIns) {
        koinInject<PublicProfileScreenRenderer>().Content(screen.slug, screen.nickname, screen.avatarUrl, checkInsOnly = true)
    }

    @Composable
    private fun PublicProfileRoute(screen: Screen.UserProfile) {
        koinInject<PublicProfileScreenRenderer>().Content(screen.slug, screen.nickname, screen.avatarUrl)
    }
}


@Composable
private fun BusinessOwnershipRoute(screen: Navigator.Screen.MyBusiness) {
    koinInject<com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry>().Content(
        onBack = Navigator::popBack,
        onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
        initialKind = screen.kind, initialSlug = screen.slug, initialId = screen.businessId,
    )
}
