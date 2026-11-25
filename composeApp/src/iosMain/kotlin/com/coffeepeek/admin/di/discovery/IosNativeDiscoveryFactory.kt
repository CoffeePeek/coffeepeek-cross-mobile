package com.coffeepeek.admin.di.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.analytics.NativeScreenAnalytics
import com.coffeepeek.admin.config.AppConfig
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.roaster.IosRoasterDiscovery
import com.coffeepeek.admin.di.roaster.RoasterAnalyticsAdapter
import com.coffeepeek.admin.di.roaster.roasterDiscoveryDestination
import com.coffeepeek.admin.di.shop.AppShopCheckInDraftStore
import com.coffeepeek.admin.location.IosLocationPermission
import com.coffeepeek.admin.location.openAppLocationSettings
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.NavMetrics
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.platformHaptic
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.discovery.api.DiscoveryRoasterContext
import com.coffeepeek.feature.discovery.api.MapFocus
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.impl.ui.*
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import com.coffeepeek.feature.roaster.impl.NativeRoasterDiscoveryHost
import com.coffeepeek.feature.roaster.impl.createNativeRoasterListSession
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.impl.createIosAppReviewPrompt
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

/** Root platform assembly only; feature sessions retain all business state and repository access. */
internal class IosNativeDiscoveryOwner(factory: NativeDiscoverySessionFactory, private val accounts: SessionReader,
    private val language: LanguagePort, private val media: IosCatalogueMedia, drafts: AppShopCheckInDraftStore,
    roasters: RoasterRepository, roasterFavorites: RoasterFavoritesPort, reselections: Flow<Unit>,
    private val mapExpanded: (Boolean) -> Unit) {
    private val core = factory.createNativeSession()
    private val binding = NativeDiscoveryIosSession(core, language.observe().map { it.languageTag })
    private val roasterSession = createNativeRoasterListSession(roasters, roasterFavorites, RoasterAnalyticsAdapter.list())
    private val searchPermission = IosLocationPermission()
    private val mapPermission = IosLocationPermission()
    private var visible = false
    private var clearance = NavMetrics.iosTabBarClearanceFloor.value
    private var resume: Pair<String, String>? = null
    private var roasterSource: IosRoasterDiscovery? = null
    private var closed = false
    val source = IosDiscovery(binding.presentations.value, media, accounts, initiallyReady = false,
        minimumBottomClearance = NavMetrics.iosTabBarClearanceFloor.value, onVisibility = ::visibility, onClearance = ::clearance)
    private val lifetime = SupervisorJob(source.scope.coroutineContext[Job])
    private val scope = CoroutineScope(source.scope.coroutineContext + lifetime)
    private val review = createIosAppReviewPrompt(scope, source::presentationHost)

    init {
        source.bindDisposal(::close)
        if (!closed) start(reselections, drafts)
    }
    private fun start(reselections: Flow<Unit>, drafts: AppShopCheckInDraftStore) {
        scope.launch { core.accountFailed.first { it }; source.fail() }
        observePresentation()
        observeRuntime()
        observeRoasters()
        scope.launch { core.events.collect(::event) }
        scope.launch { reselections.collect { core.reselect() } }
        scope.launch { Navigator.pendingMapFocus.collect { focus ->
            core.focus(focus?.let { MapFocus(it.shopId, it.title, Coordinates(it.latitude, it.longitude)) })
        } }
        scope.launch { observeCheckInResumeDestination(accounts, drafts).collect {
            resume = it
            syncPresentation()
        } }
        trackExposures()
    }

    private fun observePresentation() {
        scope.launch { combine(binding.presentations, binding.ready) { data, ready -> data to ready }
            .collect { (data, ready) -> source.update(data, ready) } }
        scope.launch { binding.filters.collect(source::updateFilters) }
        scope.launch { combine(core.runtime, binding.ready, source.screenVisible) { state, ready, visible ->
            AppConfig.storeReviewsEnabled && state.reviewEligible && ready && visible
        }.distinctUntilChanged().collect(review::presentation) }
    }
    private fun observeRuntime() {
        scope.launch { core.runtime.collect { state ->
            requestPermission(searchPermission, NativeDiscoveryPermissionTarget.Search, state.searchPermission)
            requestPermission(mapPermission, NativeDiscoveryPermissionTarget.Map, state.mapPermission)
        } }
    }
    private fun requestPermission(permission: IosLocationPermission, target: NativeDiscoveryPermissionTarget, key: Int) {
        permission.request(key,
            { core.permissionResult(target, key, NativeDiscoveryPermissionOutcome.Granted) },
            { core.permissionResult(target, key, NativeDiscoveryPermissionOutcome.Denied) },
            { core.permissionResult(target, key, NativeDiscoveryPermissionOutcome.PermanentlyDenied) })
    }
    private fun visibility(value: Boolean) {
        visible = value
        syncPresentation()
    }
    private fun clearance(value: Float) {
        clearance = value
        syncPresentation()
    }
    private fun syncPresentation() {
        if (!closed) core.presentation(visible, clearance, activeShopResumeDestination(resume, accounts) != null)
    }

    private fun event(event: NativeDiscoveryEvent) {
        when (event) {
            is NativeDiscoveryEvent.OpenShop -> Navigator.navigate(Navigator.Screen.ShopDetail(event.id))
            is NativeDiscoveryEvent.MapExpanded -> mapExpanded(event.expanded)
            NativeDiscoveryEvent.SignIn -> Navigator.navigate(Navigator.Screen.Auth)
            NativeDiscoveryEvent.AddShop -> Navigator.navigate(Navigator.Screen.AddShop)
            NativeDiscoveryEvent.AddRoaster -> Navigator.navigate(Navigator.Screen.AddRoaster)
            NativeDiscoveryEvent.Back -> Navigator.popBack()
            NativeDiscoveryEvent.Close -> Navigator.popBack()
            NativeDiscoveryEvent.ContinueCheckIn -> resumeShopCheckIn(resume, accounts)
            NativeDiscoveryEvent.FocusConsumed -> Navigator.consumeMapFocus()
            NativeDiscoveryEvent.OpenLocationSettings -> openAppLocationSettings()
            NativeDiscoveryEvent.FavoriteFeedback -> platformHaptic(HapticEvent.Selection)
        }
    }

    private fun observeRoasters() {
        scope.launch { core.runtime.map { it.roasters }.distinctUntilChanged().collect { context ->
            roasterSession.criteria(context.query, context.selectedRoasterIds, context.favoritesOnly)
        } }
        scope.launch { combine(core.runtime.map { it.roasters }.distinctUntilChanged(), roasterSession.state) { context, _ ->
            roasterSession.hasResults(context.query, context.selectedRoasterIds, context.favoritesOnly)
        }.distinctUntilChanged().collect(core::roasterResults) }
        scope.launch { binding.presentations.map { it.state }.collect { updateRoasterSource(it, core.currentRuntime().roasters) } }
    }
    private fun updateRoasterSource(state: DiscoveryIosState, context: DiscoveryRoasterContext) {
        val shown = state.roastersExpanded || (!state.mapExpanded && (state.landing || state.showRoasterResults))
        if (!shown) {
            roasterSource?.close()
            roasterSource = null
            source.roastersChanged(null)
            return
        }
        val host = roasterHost(context, state)
        if (roasterSource?.search != state.roastersExpanded) {
            roasterSource?.close()
            roasterSource = IosRoasterDiscovery(roasterSession, host, language, accounts, media).also(::trackRoasterExposures)
        }
        roasterSource?.update(host)
        roasterSource?.setVisible(state.active && visible)
        source.roastersChanged(roasterSource)
    }
    private fun roasterHost(context: DiscoveryRoasterContext, state: DiscoveryIosState) = NativeRoasterDiscoveryHost(
        context.query, context.selectedRoasterIds, context.favoritesOnly, state.roastersExpanded,
        { Navigator.navigate(roasterDiscoveryDestination(it, accounts)) }, { Navigator.navigate(Navigator.Screen.AddRoaster) },
        core::query, { core.perform("cancel-roasters", "") }, { platformHaptic(HapticEvent.Selection) }, Modifier, state.bottomClearance)

    private fun trackExposures() {
        val tracker = NativeScreenAnalytics(scope, null, source.screenVisible)
        listOf("search" to "search", "filters" to "filters", "shops" to "shop_catalog", "favorite" to "favorites").forEach { (key, feature) ->
            tracker.feature(feature, if (key == "shops") "discovery" else if (key == "favorite") "catalog_card" else "shops",
                combine(source.visibleElements, binding.presentations) { elements, data -> key in elements && data.state.active && !data.state.roastersExpanded })
        }
        listOf(false, true).forEach { expanded -> tracker.feature("map", if (expanded) "discovery" else "home",
            combine(source.visibleElements, binding.presentations) { elements, data ->
                "map" in elements && data.state.active && !data.state.roastersExpanded && data.state.mapExpanded == expanded
            }) }
    }
    private fun trackRoasterExposures(child: IosRoasterDiscovery) {
        val tracker = NativeScreenAnalytics(child.scope, null, combine(child.screenVisible, source.screenVisible) { child, parent -> child && parent })
        tracker.feature("roaster_catalog", if (child.search) "catalog" else "discovery", child.fractions.map { (it["catalog"] ?: 0.0) >= .1 })
        if (child.search) tracker.feature("search", "roasters", child.fractions.map { (it["search"] ?: 0.0) >= .1 })
        tracker.feature("favorites", "catalog_card", child.fractions.map { it.any { (key, value) -> key.startsWith("favorite:") && value >= .1 } })
    }
    private fun close() {
        if (closed) return
        closed = true
        lifetime.cancel()
        searchPermission.close()
        mapPermission.close()
        review.close()
        roasterSource?.close()
        roasterSource = null
        roasterSession.close()
        binding.close()
        mapExpanded(false)
    }
}

@Composable
internal fun iosDiscoveryContent(mapExpanded: (Boolean) -> Unit, reselections: Flow<Unit>): () -> UIViewController {
    val factory = koinInject<NativeDiscoverySessionFactory>()
    val accounts = koinInject<SessionReader>()
    val language = koinInject<LanguagePort>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val drafts = koinInject<AppShopCheckInDraftStore>()
    val roasters = koinInject<RoasterRepository>()
    val favorites = koinInject<RoasterFavoritesPort>()
    return remember(factory, accounts, language, fetcher, drafts, roasters, favorites, mapExpanded, reselections) {
        { discoveryControllerFactory(IosNativeDiscoveryOwner(factory, accounts, language, IosCatalogueMedia(fetcher, Constants.BASE_URL),
            drafts, roasters, favorites, reselections, mapExpanded).source) }
    }
}
