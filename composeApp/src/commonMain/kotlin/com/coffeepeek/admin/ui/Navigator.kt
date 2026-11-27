package com.coffeepeek.admin.ui

import com.coffeepeek.core.designsystem.theme.reducedMotionDuration
import com.coffeepeek.admin.di.collections.CollectionsComposerDestination
import com.coffeepeek.admin.di.collections.PublicCollectionDestination
import com.coffeepeek.admin.di.collections.CollectionsLibraryDestination
import com.coffeepeek.admin.di.collections.CollectionEditorDestination
import com.coffeepeek.admin.di.collections.SavedPublicCollectionDestination
import com.coffeepeek.admin.di.homecheckin.GuestCheckInOnboardingHost

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.screen.checkins.HomeCheckInDetailDestination
import com.coffeepeek.admin.ui.screen.shelf.ShelfScreenRenderer
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.admin.ui.screen.contributions.ContributionsScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.AccountScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.PublicProfileScreenRenderer
import com.coffeepeek.admin.ui.screen.roaster.RoasterScreenRenderer
import com.coffeepeek.admin.ui.screen.community.CheckInDetailScreenRenderer
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import org.koin.compose.koinInject
import com.coffeepeek.admin.ui.dialogs.ErrorDialog
import com.coffeepeek.admin.ui.dialogs.LoadingDialog
import com.coffeepeek.admin.ui.screen.auth.AuthScreenRenderer
import com.coffeepeek.core.session.SessionReader
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
import kotlinx.coroutines.flow.map
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

/** The root navigation host: slide transitions honor the reduce-motion setting in one place. */
@Composable
private fun RootNavHost(
    nav: NavHostController,
    modifier: Modifier,
    builder: androidx.navigation.NavGraphBuilder.() -> Unit,
) {
    val rootNavDuration = reducedMotionDuration(ROOT_NAV_ANIMATION_DURATION_MS)
    NavHost(
        navController = nav,
        startDestination = Screen.Main,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = rootNavDuration, easing = FastOutSlowInEasing),
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(durationMillis = rootNavDuration, easing = FastOutSlowInEasing),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = rootNavDuration, easing = FastOutSlowInEasing),
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(durationMillis = rootNavDuration, easing = FastOutSlowInEasing),
            )
        },
        builder = builder,
    )
}

    private val navigatorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _navigationEvents = MutableSharedFlow<NavEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed interface NavEvent {
        data class NavigateTo(val screen: Screen) : NavEvent
        data class SelectTab(val tab: Screen) : NavEvent
        data object PopBack : NavEvent
        data object NavigateUp : NavEvent
        class ReturnToProfile internal constructor(internal val origin: RootNavigationOrigin) : NavEvent
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
        @Serializable data object CommunityGraph : Screen
        @Serializable data object ProfileGraph : Screen
        @Serializable data object BusinessGraph : Screen
        @Serializable data object BusinessTab : Screen
        @Serializable data object SettingsGraph : Screen

        // Tabs
        @Serializable data object FeedTab : Screen
        @Serializable data object CommunityTab : Screen
        @Serializable data object ProfileTab : Screen
        @Serializable data object SettingsTab : Screen

        // Inner screens (add here + in the graph in MainScreen)
        @Serializable data class ShopDetail(
            val shopId: String, val openCheckIn: Boolean = false,
        ) : Screen
        @Serializable data class ShopCheckInSignIn(val shopId: String) : Screen
        @Serializable data object CreateCheckIn : Screen
        @Serializable data class ShopCheckInEditor(val shopId: String) : Screen
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
        @Serializable data object CollectionsComposer : Screen
        @Serializable data class PublicCollection(val id: String) : Screen
        @Serializable data object CollectionsLibrary : Screen
        @Serializable data class CollectionEditor(val id: String? = null) : Screen
        @Serializable data class SavedPublicCollection(val id: String) : Screen
        @Serializable data class HomeCheckInDetail(val id: String) : Screen
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

    private val _pendingProfileShelf = MutableStateFlow(false)
    val pendingProfileShelf = _pendingProfileShelf.asStateFlow()

    private val _pendingProfileCheckIns = MutableStateFlow(false)
    val pendingProfileCheckIns = _pendingProfileCheckIns.asStateFlow()

    private val _pendingProfileCollections = MutableStateFlow(false)
    val pendingProfileCollections = _pendingProfileCollections.asStateFlow()

    fun consumeProfileShelf() {
        _pendingProfileShelf.value = false
    }

    fun consumeProfileCheckIns() {
        _pendingProfileCheckIns.value = false
    }

    fun consumeProfileCollections() {
        _pendingProfileCollections.value = false
    }

    /** Explicit history intent; ordinary profile tab selection retains its current section and date. */
    fun openProfileCheckIns(popCurrent: Boolean = false) {
        consumeProfileShelf()
        consumeProfileCollections()
        _pendingProfileCheckIns.value = true
        if (popCurrent) popThenSelectTab(Screen.ProfileTab) else selectTab(Screen.ProfileTab)
    }

    private fun clearSessionNavigationRequests() {
        hasOpenedAppLink.value = false
        consumeProfileShelf()
        consumeProfileCheckIns()
        consumeProfileCollections()
        consumeTabSelection()
    }

    fun returnToProfileShelf() {
        consumeProfileCheckIns()
        consumeProfileCollections()
        _pendingProfileShelf.value = true
        popThenSelectTab(Screen.ProfileTab)
    }

    private val _openLoginAfterSessionEnd = MutableStateFlow(false)
    internal val nativeLoginAfterSessionEnd = _openLoginAfterSessionEnd.asStateFlow()
    internal fun consumeNativeLoginAfterSessionEnd() = _openLoginAfterSessionEnd.compareAndSet(true, false)
    private val pendingAppLink = MutableStateFlow<Screen?>(null)
    // Keep the guide out of an app-link launch after the destination consumes the pending route.
    private val hasOpenedAppLink = MutableStateFlow(false)
    internal val nativePendingLink = pendingAppLink.asStateFlow()
    internal val nativeOpenedLink = hasOpenedAppLink.asStateFlow()
    private val checkInSignInHandoff = ShopCheckInSignInHandoff()

    internal fun shopCheckInSignInAttempt(shopId: String, sessions: SessionReader): ShopCheckInSignInHandoff.Attempt =
        checkInSignInHandoff.attempt(shopId) { sessions.isActiveSession(sessions.peekSession()) }

    internal fun openAppLink(screen: Screen) {
        hasOpenedAppLink.value = true
        pendingAppLink.value = screen
    }

    fun consumeMapFocus() {
        _pendingMapFocus.value = null
    }

    fun consumeTabSelection() {
        _pendingTabSelection.value = null
    }

    internal fun consumeNativeTab(expected: Screen) = _pendingTabSelection.compareAndSet(expected, null)
    internal fun consumeNativeLink(expected: Screen) = pendingAppLink.compareAndSet(expected, null)

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
        is Screen.CollectionsComposer,
        is Screen.PublicCollection,
        is Screen.CollectionsLibrary,
        is Screen.CollectionEditor,
        is Screen.SavedPublicCollection,
        is Screen.HomeCheckInDetail,
        is Screen.ShelfArchive,
        is Screen.ShelfAddBag,
        is Screen.ShelfBag,
        is Screen.EditHomeCheckIn,
        is Screen.CitySettings,
        is Screen.ThemeSettings,
        is Screen.LanguageSettings,
        is Screen.BlockedAuthors,
        is Screen.UsagePolicy -> true
        else -> isCheckInRootRoute()
    }

    private fun Screen.isCheckInRootRoute(): Boolean =
        this is Screen.ShopCheckInSignIn || this is Screen.CreateCheckIn || this is Screen.ShopCheckInEditor

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

    internal fun returnToProfile(origin: RootNavigationOrigin) {
        navigatorScope.launch { _navigationEvents.emit(NavEvent.ReturnToProfile(origin)) }
    }

    private fun prepareProfileReturn() {
        consumeProfileShelf()
        consumeProfileCheckIns()
        consumeTabSelection()
        _pendingProfileCollections.value = true
    }

    internal fun prepareRootEvent(event: NavEvent, origin: RootNavigationOrigin, session: RootNavigationSession): Boolean {
        if (!origin.accepts(event, session)) return false
        if (event is NavEvent.ReturnToProfile) prepareProfileReturn()
        return true
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

    internal fun prepareRootTransition(transition: RootSessionTransition) {
        if (transition.resetsNavigation) clearPlatformNavigationState()
        if (transition.clearsPrivateRequests) clearSessionNavigationRequests()
    }

    internal suspend fun acceptRootTransition(transition: RootSessionTransition) {
        if (transition.current.isLoggedIn) {
            checkInSignInHandoff.take()?.let { pendingAppLink.compareAndSet(null, it) }
        } else {
            ErrorHandler.clearError()
            LoadingHandler.clearLoading()
        }
    }

    @Composable
    operator fun invoke(
        vm: NavigatorViewModel = platformViewModel(),
    ) {
        val errorMessage = ErrorHandler.errorMessage.collectAsState().value
        val loading = LoadingHandler.isLoading.collectAsState().value
        val state by vm.navigationState.collectAsState()

        ErrorDialog(
            show = errorMessage != null,
            message = errorMessage ?: "",
            onDismiss = { ErrorHandler.clearError() }
        )
        NavigationContent(vm, state, loading)
    }

    @Composable
    private fun NavigationContent(
        vm: NavigatorViewModel,
        state: RootNavigationState,
        loading: Boolean,
    ) {
        val openedAppLink by hasOpenedAppLink.collectAsState()
        val isLoggedIn = state.session?.isLoggedIn
        DeferredLogin(vm, isLoggedIn)
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoggedIn != null) {
                key(state.generation, isLoggedIn) {
                    RootNavigationHost(checkNotNull(state.session))
                }
            }
            LoadingDialog(show = loading)
            if (isLoggedIn == false && !openedAppLink && !state.onboardingDismissed) {
                GuestCheckInOnboardingHost { destination ->
                    if (vm.dismissOnboarding()) destination?.let(::navigate)
                }
            }
        }
    }

    @Composable
    private fun DeferredLogin(vm: NavigatorViewModel, isLoggedIn: Boolean?) {
        val requested by _openLoginAfterSessionEnd.collectAsState()
        LaunchedEffect(isLoggedIn, requested) {
            if (isLoggedIn == false && requested) {
                vm.dismissOnboarding()
                yield()
                if (consumeNativeLoginAfterSessionEnd()) navigate(Screen.Auth)
            }
        }
    }

    @Composable
    private fun RootNavigationHost(session: RootNavigationSession) {
        val origin = remember(session) { RootNavigationOrigin(session) }
        CompositionLocalProvider(LocalRootNavigationOrigin provides origin) { BaseNavigator(session.isLoggedIn) }
    }

    private fun rootNavigationEvents(isLoggedIn: Boolean) = navigationEvents.map { event ->
        if (event is NavEvent.NavigateTo) event.copy(screen = event.screen.profileDestination(isLoggedIn)) else event
    }

    @Composable
    private fun BaseNavigator(isLoggedIn: Boolean) {
        koinInject<com.coffeepeek.admin.ui.screen.notifications.NotificationsRenderer>().HandlePendingOpen(isLoggedIn)
        val appLink by pendingAppLink.collectAsState()
        val origin = LocalRootNavigationOrigin.current
        val sessions = koinInject<SessionReader>()
        val sessionEvents = remember(isLoggedIn) { rootNavigationEvents(isLoggedIn) }
        if (PlatformRootNavigation(isLoggedIn, sessionEvents, appLink?.appLinkDestination(isLoggedIn),
            { pendingAppLink.compareAndSet(appLink, null) },
            { screen, login -> RootScreen(screen, login, isLoggedIn) },
        )) return

        val nav = rememberNavController()
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
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
            if (screen == Screen.ProfileTab) {
                nav.popBackStack<Screen.Main>(inclusive = false)
                selectTab(Screen.ProfileTab)
            } else nav.navigate(screen.appLinkDestination(isLoggedIn)) {
                launchSingleTop = true
                popUpTo<Screen.Main>()
            }
            pendingAppLink.compareAndSet(screen, null)
        }

        LaunchedEffect(Unit) {
            sessionEvents.onEach { nav.handleRootEvent(it, origin, sessions) }.launchIn(this)
        }

        val shopPhotoUnderStatusBar = currentEntry?.destination?.let {
            it.hasRoute<Screen.ShopDetail>() || it.hasRoute<Screen.RoasterDetail>()
        } == true
        val authUnderSystemBars = currentEntry?.destination?.let {
            it.hasRoute<Screen.Auth>() || it.hasRoute<Screen.Register>() || it.hasRoute<Screen.ShopCheckInSignIn>()
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
            RootNavHost(
                nav,
                Modifier.fillMaxSize().windowInsetsPadding(
                    if (shopPhotoUnderStatusBar || authUnderSystemBars) WindowInsets(0)
                    else systemInsets.only(WindowInsetsSides.Top),
                ).clipToBounds(),
            ) {
                    composable<Screen.Auth> { koinInject<AuthScreenRenderer>().Login() }
                    composable<Screen.Register> { koinInject<AuthScreenRenderer>().Register() }
                    composable<Screen.ShopCheckInSignIn> { backStack ->
                        ShopCheckInSignInRoute(backStack.toRoute<Screen.ShopCheckInSignIn>().shopId)
                    }
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
                        koinInject<ShopDetailScreenRenderer>().Content(route.shopId, route.openCheckIn)
                    }
                    composable<Screen.CreateCheckIn> { CheckInRootScreen(Screen.CreateCheckIn) }
                    composable<Screen.ShopCheckInEditor> { ShopCheckInEditorRoute(it.toRoute<Screen.ShopCheckInEditor>().shopId) }
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
                        org.koin.compose.koinInject<com.coffeepeek.admin.ui.screen.review.ContentReportScreenRenderer>().Content(route.reviewId, false, route.isPreview)
                    }
                    composable<Screen.ReportCheckIn> { backStack ->
                        val route = backStack.toRoute<Screen.ReportCheckIn>()
                        org.koin.compose.koinInject<com.coffeepeek.admin.ui.screen.review.ContentReportScreenRenderer>().Content(route.checkInId, true, false)
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
                    composable<Screen.CollectionsComposer> { CollectionsComposerDestination() }
                    composable<Screen.PublicCollection> { backStack -> PublicCollectionDestination(backStack.toRoute<Screen.PublicCollection>().id) }
                    composable<Screen.CollectionsLibrary> { CollectionsLibraryDestination() }
                    composable<Screen.CollectionEditor> { backStack -> CollectionEditorDestination(backStack.toRoute<Screen.CollectionEditor>().id) }
                    composable<Screen.SavedPublicCollection> { backStack -> SavedPublicCollectionDestination(backStack.toRoute<Screen.SavedPublicCollection>().id) }
                    homeCheckInRoutes()
                    composable<Screen.ShelfArchive> { koinInject<ShelfScreenRenderer>().Archive() }
                    composable<Screen.ShelfAddBag> { koinInject<ShelfScreenRenderer>().AddBag() }
                    composable<Screen.ShelfBag> { backStack ->
                        val route = backStack.toRoute<Screen.ShelfBag>()
                        koinInject<ShelfScreenRenderer>().Bag(route.bagId)
                    }
                    composable<Screen.CitySettings> { koinInject<SettingsScreenRenderer>().City() }
                    composable<Screen.ThemeSettings> { koinInject<SettingsScreenRenderer>().Theme() }
                    composable<Screen.LanguageSettings> { koinInject<SettingsScreenRenderer>().Language() }
                    composable<Screen.BlockedAuthors> { com.coffeepeek.admin.di.authorblocking.AuthorBlockingScreenRoute() }
                    composable<Screen.UsagePolicy> { koinInject<SettingsScreenRenderer>().UsagePolicy() }
            }
        }
    }

    private fun androidx.navigation.NavGraphBuilder.homeCheckInRoutes() {
        composable<Screen.HomeCheckInDetail> { entry ->
            HomeCheckInDetailDestination(entry.toRoute<Screen.HomeCheckInDetail>().id)
        }
        composable<Screen.EditHomeCheckIn> { entry ->
            HomeCheckInDetailDestination(entry.toRoute<Screen.EditHomeCheckIn>().id, editOnLoad = true)
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
            is Screen.ShopDetail -> koinInject<ShopDetailScreenRenderer>().Content(screen.shopId, screen.openCheckIn)
            is Screen.ShopCheckInSignIn -> ShopCheckInSignInRoute(screen.shopId)
            Screen.CreateCheckIn, is Screen.ShopCheckInEditor -> CheckInRootScreen(screen)
            is Screen.CoffeeDetail -> koinInject<CoffeeScreenRenderer>().Detail(screen.slug)
            is Screen.ShopReviews -> koinInject<ShopCheckInListScreenRenderer>().Content(screen.shopId)
            is Screen.ReportReview -> org.koin.compose.koinInject<com.coffeepeek.admin.ui.screen.review.ContentReportScreenRenderer>().Content(screen.reviewId, false, screen.isPreview)
            is Screen.ReportCheckIn -> org.koin.compose.koinInject<com.coffeepeek.admin.ui.screen.review.ContentReportScreenRenderer>().Content(screen.checkInId, true, false)
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
            Screen.CollectionsComposer -> CollectionsComposerDestination()
            is Screen.PublicCollection -> PublicCollectionDestination(screen.id)
            Screen.CollectionsLibrary -> CollectionsLibraryDestination()
            is Screen.CollectionEditor -> CollectionEditorDestination(screen.id)
            is Screen.SavedPublicCollection -> SavedPublicCollectionDestination(screen.id)
            is Screen.HomeCheckInDetail -> HomeCheckInDetailDestination(screen.id)
            Screen.ShelfArchive -> koinInject<ShelfScreenRenderer>().Archive()
            Screen.ShelfAddBag -> koinInject<ShelfScreenRenderer>().AddBag()
            is Screen.ShelfBag -> koinInject<ShelfScreenRenderer>().Bag(screen.bagId)
            is Screen.EditHomeCheckIn -> HomeCheckInDetailDestination(screen.id, editOnLoad = true)
            Screen.CitySettings -> koinInject<SettingsScreenRenderer>().City()
            Screen.ThemeSettings -> koinInject<SettingsScreenRenderer>().Theme()
            Screen.LanguageSettings -> koinInject<SettingsScreenRenderer>().Language()
            Screen.BlockedAuthors -> com.coffeepeek.admin.di.authorblocking.AuthorBlockingScreenRoute()
            Screen.UsagePolicy -> koinInject<SettingsScreenRenderer>().UsagePolicy()
            else -> error("Tab route must be handled by MainScreen: $screen")
        }
    }

    @Composable
    private fun CheckInRootScreen(screen: Screen) {
        when (screen) {
            Screen.CreateCheckIn -> koinInject<com.coffeepeek.admin.ui.screen.checkins.CheckInShopPickerRenderer>().Content()
            is Screen.ShopCheckInEditor -> ShopCheckInEditorRoute(screen.shopId)
            else -> error("Expected a check-in route: $screen")
        }
    }

    @Composable
    private fun ShopCheckInEditorRoute(shopId: String) {
        koinInject<com.coffeepeek.admin.ui.screen.shop.ShopCheckInEditorRenderer>().Content(shopId)
    }

    @Composable
    private fun ShopCheckInSignInRoute(shopId: String) {
        val sessions = koinInject<SessionReader>()
        val attempt = remember(shopId, sessions) { shopCheckInSignInAttempt(shopId, sessions) }
        DisposableEffect(attempt) {
            attempt.begin()
            onDispose(attempt::dispose)
        }
        koinInject<AuthScreenRenderer>().CheckInLogin(onClose = {
            attempt.cancel()
            popBack()
        })
    }

    private fun NavHostController.navigateFromRoot(screen: Screen) {
        val stack = currentBackStack.value.map { entry ->
            if (entry.destination.hasRoute<Screen.UserProfile>()) entry.toRoute<Screen.UserProfile>() else null
        }
        val existing = stack.getOrNull(existingProfileIndex(stack, screen))
        if (existing != null) popBackStack(existing, inclusive = false) else navigate(screen)
    }

    private fun NavHostController.handleRootEvent(event: NavEvent, origin: RootNavigationOrigin, sessions: SessionReader) {
        if (!prepareRootEvent(event, origin, sessions.currentRootNavigationSession())) return
        when (event) {
            NavEvent.PopBack -> popBackStack()
            NavEvent.NavigateUp -> navigateUp()
            is NavEvent.ReturnToProfile -> {
                popBackStack<Screen.Main>(inclusive = false)
                _pendingTabSelection.value = Screen.ProfileTab
            }
            is NavEvent.NavigateTo -> if (event.screen.isHandledByRootNav()) navigateFromRoot(event.screen)
            is NavEvent.SelectTab -> Unit
        }
    }

    @Composable
    private fun PublicCheckInsRoute(screen: Screen.UserCheckIns) {
        koinInject<PublicProfileScreenRenderer>().Content(screen.slug, screen.nickname, screen.avatarUrl, checkInsOnly = true)
    }

    @Composable
    private fun PublicProfileRoute(screen: Screen.UserProfile) {
        koinInject<PublicProfileScreenRenderer>().Content(screen.slug, screen.nickname, screen.avatarUrl, checkInsOnly = false)
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
