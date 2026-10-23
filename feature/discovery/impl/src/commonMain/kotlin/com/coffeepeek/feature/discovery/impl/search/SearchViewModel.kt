package com.coffeepeek.feature.discovery.impl.search

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.discovery.api.DiscoveryAccount
import com.coffeepeek.feature.discovery.api.DiscoveryAnalytics
import com.coffeepeek.feature.discovery.api.DiscoveryCity
import com.coffeepeek.feature.discovery.api.DiscoveryFavorites
import com.coffeepeek.feature.discovery.api.DiscoveryGate
import com.coffeepeek.feature.discovery.api.DiscoveryLocation
import com.coffeepeek.feature.discovery.api.FilterAction
import com.coffeepeek.feature.discovery.api.GeolocationOutcome
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFailure
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.ShopQuery
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import com.coffeepeek.feature.discovery.domain.rules.searchText
import com.coffeepeek.feature.discovery.domain.rules.withFavoriteMembership
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val PAGE_SIZE = 20
private const val QUERY_DEBOUNCE_MS = 400L
private const val PLACEMENT = "discovery"

/**
 * The shop search as the legacy feed kept it (the debounced query, the filters, the city, nearby, paging, the optimistic favorite), with three differences.
 * One account at a time: a change of account drops the visited and favorites filters and reloads, a guest is sent to sign in for them. A query typed in the first
 * debounce window is no longer swallowed. And a failed search shows the server's message or the default text.
 */
@OptIn(FlowPreview::class)
internal class SearchViewModel(
    private val repository: DiscoveryRepository,
    private val favorites: DiscoveryFavorites,
    private val city: DiscoveryCity,
    private val location: DiscoveryLocation,
    account: DiscoveryAccount,
    private val analytics: DiscoveryAnalytics,
) : MviViewModel<SearchState, SearchAction, SearchEvent>(SearchState()) {
    private val queries = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private val favoriteGuard = Mutex()
    private var origin: Coordinates? = null
    private var cityReady = false
    private var accountSeen = false
    private var generation = 0L
    private var favoriteIds: Set<String>? = null
    private var requestedText: String? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch { account.accountKey.distinctUntilChanged().collect(::onAccount) }
        viewModelScope.launch { favorites.ids.collect(::onFavoriteIds) }
        viewModelScope.launch { city.selected.collect(::onCity) }
        viewModelScope.launch { location.current.collect(::onLocation) }
        viewModelScope.launch {
            queries.debounce(QUERY_DEBOUNCE_MS).collect { text -> if (searchText(text) != requestedText) loadShops(reset = true) }
        }
        loadCatalogs()
    }

    override suspend fun handleActionInternal(action: SearchAction) {
        when (action) {
            is SearchAction.Query -> { updateState { copy(query = action.text) }; queries.tryEmit(action.text) }
            SearchAction.ActivateSearch -> updateState { copy(searchActive = true) }
            SearchAction.CancelSearch -> {
                if (currentState.activeFilterCount > 0) clearFilters() else setQuery("")
                updateState { copy(searchActive = false) }
            }
            SearchAction.ToggleFilters -> toggleFilters()
            SearchAction.CloseFilters -> updateState { copy(showFilters = false) }
            is SearchAction.ApplyFilters -> applyFilters(action.filters)
            is SearchAction.SetKind -> changeFilters { copy(kind = action.kind) }
            SearchAction.ToggleOpen -> changeFilters(reloadOnlyWithNearby = true) { copy(openOnly = !openOnly) }
            SearchAction.ToggleNew -> changeFilters(reloadOnlyWithNearby = true) { copy(newOnly = !newOnly) }
            SearchAction.ToggleVisited -> if (guarded(DiscoveryGate.VisitedFilter)) changeFilters(reloadOnlyWithNearby = true) { copy(visitedOnly = !visitedOnly) }
            SearchAction.ToggleFavorites -> if (guarded(DiscoveryGate.FavoritesFilter)) changeFilters(reloadOnlyWithNearby = true) { copy(favoritesOnly = !favoritesOnly) }
            SearchAction.ToggleNearby -> toggleNearby()
            SearchAction.LocationGranted -> location.refresh()
            SearchAction.LocationDenied -> analytics.geolocation(PLACEMENT, userTriggered = false, outcome = GeolocationOutcome.Unavailable)
            is SearchAction.SetPrice -> changeFilters { copy(priceLevel = action.level) }
            is SearchAction.SetMinRating -> changeFilters { copy(minRating = action.rating) }
            is SearchAction.ToggleCatalog -> changeFilters {
                when (action.kind) {
                    CatalogKind.Roaster -> copy(roasterIds = roasterIds.toggle(action.id))
                    CatalogKind.Bean -> copy(beanIds = beanIds.toggle(action.id))
                    CatalogKind.Equipment -> copy(equipmentIds = equipmentIds.toggle(action.id))
                    CatalogKind.BrewMethod -> copy(brewMethodIds = brewMethodIds.toggle(action.id))
                    CatalogKind.Tag -> copy(tagIds = tagIds.toggle(action.id))
                }
            }
            SearchAction.ClearFilters -> clearFilters()
            SearchAction.LoadMore -> loadShops(reset = false)
            SearchAction.Refresh -> if (!currentState.isRefreshing && !currentState.isLoading) loadShops(reset = true)
            is SearchAction.ToggleFavorite -> toggleFavorite(action.shopId)
            is SearchAction.OpenShop -> {
                analytics.shopOpened(currentState.visibleShops.indexOfFirst { it.id == action.shopId }.takeIf { it >= 0 }?.plus(1))
                sendEvent(SearchEvent.OpenShop(action.shopId))
            }
        }
    }

    // ---- what comes from outside ----

    private fun onAccount(key: String?) {
        val first = !accountSeen
        accountSeen = true
        updateState { copy(isGuest = key == null) }
        if (first) return
        // Another account's visited and favorites are not this account's: the filters go, the page is read again.
        generation++
        updateState { copy(filters = filters.copy(visitedOnly = false, favoritesOnly = false), favoriteUpdates = emptySet()) }
        if (cityReady) loadShops(reset = true)
    }

    private fun onFavoriteIds(ids: Set<String>) {
        favoriteIds = ids
        updateState { copy(shops = withFavoriteMembership(shops, ids)) }
    }

    private fun onCity(cityId: String?) {
        if (!cityReady || cityId == null || cityId == currentState.filters.cityId) return
        updateState { copy(filters = filters.copy(cityId = cityId)) }
        loadShops(reset = true)
    }

    private fun onLocation(point: Coordinates?) {
        if (point == null || !point.isValid || point == origin) return
        origin = point
        analytics.geolocation(PLACEMENT, userTriggered = false, outcome = GeolocationOutcome.Available)
        updateState { copy(canUseNearby = true) }
        if (currentState.filters.nearbyOnly) {
            updateState { copy(shops = emptyList()) }
            if (cityReady) loadShops(reset = true)
        }
    }

    private fun loadCatalogs() {
        viewModelScope.launch {
            repository.catalogs().onSuccess { catalogs ->
                val cityId = city.resolve(catalogs.cities)
                updateState { copy(catalogs = catalogs, filters = filters.copy(cityId = cityId)) }
            }
            // A failed catalog still lets the first search start; the filter sheet asks for the catalogs again.
            cityReady = true
            loadShops(reset = true)
        }
    }

    // ---- filters ----

    private fun toggleFilters() {
        val opening = !currentState.showFilters
        if (opening) analytics.filtersOpened()
        updateState { copy(showFilters = opening) }
        if (opening && currentState.catalogs.cities.isEmpty() && currentState.catalogs.beans.isEmpty()) loadCatalogs()
    }

    private fun applyFilters(filters: DiscoveryFilters) {
        val before = currentState.filters.analyticsValues()
        val after = filters.analyticsValues()
        analytics.filtersChanged(after.keys.sorted(), changedFilterKeys(before, after), FilterAction.Apply)
        updateState { copy(filters = filters, shops = if (this.filters.nearbyOnly != filters.nearbyOnly) emptyList() else shops) }
        loadShops(reset = true)
    }

    private fun changeFilters(reloadOnlyWithNearby: Boolean = false, transform: DiscoveryFilters.() -> DiscoveryFilters) {
        val before = currentState.filters
        updateState { copy(filters = filters.transform()) }
        trackChange(before)
        if (!reloadOnlyWithNearby || currentState.filters.nearbyOnly) loadShops(reset = true)
    }

    private fun toggleNearby() {
        if (!currentState.canUseNearby) return
        val before = currentState.filters
        val enabling = !before.nearbyOnly
        updateState { copy(filters = filters.copy(nearbyOnly = enabling), shops = emptyList()) }
        trackChange(before)
        loadShops(reset = true)
        if (enabling) location.refresh()
    }

    private fun clearFilters() {
        val before = currentState.filters.analyticsValues()
        val after = currentState.filters.clearSelections().analyticsValues()
        analytics.filtersChanged(after.keys.sorted(), changedFilterKeys(before, after), FilterAction.Reset)
        queries.tryEmit("")
        updateState { copy(query = "", filters = filters.clearSelections(), shops = if (filters.nearbyOnly) emptyList() else shops) }
        loadShops(reset = true)
    }

    private fun setQuery(text: String) {
        updateState { copy(query = text) }
        queries.tryEmit(text)
    }

    private fun trackChange(before: DiscoveryFilters) {
        val old = before.analyticsValues()
        val new = currentState.filters.analyticsValues()
        val changed = changedFilterKeys(old, new)
        if (changed.isNotEmpty()) analytics.filtersChanged(new.keys.sorted(), changed, FilterAction.Apply)
    }

    /** A guest cannot use this switch: the gate is reported and the sign-in screen asked for. */
    private suspend fun guarded(gate: DiscoveryGate): Boolean {
        if (!currentState.isGuest) return true
        analytics.gateShown(gate)
        sendEvent(SearchEvent.SignInRequired)
        return false
    }

    // ---- the list ----

    private fun loadShops(reset: Boolean) {
        val snapshot = currentState
        if (!reset && (snapshot.isLoadingMore || !snapshot.hasMore || snapshot.isLoading || snapshot.isRefreshing)) return
        loadJob?.cancel()
        val mine = generation
        loadJob = viewModelScope.launch {
            val page = if (reset) 1 else currentState.currentPage + 1
            updateState {
                when {
                    reset && shops.isEmpty() -> copy(isLoading = true, isRefreshing = false, isLoadingMore = false, error = null)
                    reset -> copy(isRefreshing = true, isLoading = false, isLoadingMore = false, error = null)
                    else -> copy(isLoadingMore = true, error = null)
                }
            }
            val state = currentState
            val filters = state.filters
            val nearby = filters.nearbyOnly && origin != null
            val run = if (reset) {
                requestedText = searchText(state.query)
                analytics.searchStarted(state.query.trim(), filters.analyticsValues().keys.toList(), nearby, citySlug = filters.cityId, criteria = state.query.normalizedQuery() to filters)
            } else null
            // Nearby ignores the city: the distance is the scope.
            val query = ShopQuery(state.query, if (nearby) filters.copy(cityId = null) else filters, origin.takeIf { nearby }, page, PAGE_SIZE)
            repository.search(query).onSuccess { result ->
                if (mine != generation) return@launch
                run?.results(result.totalCount)
                updateState {
                    val loaded = if (reset) result.items else shops + result.items
                    copy(
                        shops = favoriteIds?.let { withFavoriteMembership(loaded, it) } ?: loaded,
                        currentPage = result.currentPage, totalPages = result.totalPages, hasMore = result.hasMore,
                        isLoading = false, isRefreshing = false, isLoadingMore = false, error = null,
                    )
                }
            }.onFailure { failure ->
                if (mine != generation) return@launch
                run?.failed(failure)
                updateState { copy(isLoading = false, isRefreshing = false, isLoadingMore = false, error = SearchError((failure as? DiscoveryFailure)?.serverMessage)) }
            }
        }
    }

    // ---- favorites ----

    private suspend fun toggleFavorite(shopId: String) {
        if (currentState.isGuest) { sendEvent(SearchEvent.SignInRequired); return }
        val shop = currentState.shops.firstOrNull { it.id == shopId } ?: return
        val started = favoriteGuard.withLock {
            if (shopId in currentState.favoriteUpdates) false else { updateState { copy(favoriteUpdates = favoriteUpdates + shopId) }; true }
        }
        if (!started) return
        val previous = shop.isFavorite
        val next = !previous
        updateState { copy(shops = shops.map { if (it.id == shopId) it.copy(isFavorite = next) else it }) }
        try {
            if (favorites.setFavorite(shop, next).isFailure) restoreFavorite(shopId, previous)
        } catch (cancelled: CancellationException) {
            restoreFavorite(shopId, previous)
            throw cancelled
        } catch (error: Exception) {
            restoreFavorite(shopId, previous)
        } finally {
            favoriteGuard.withLock { updateState { copy(favoriteUpdates = favoriteUpdates - shopId) } }
        }
    }

    /** Back to what it was, unless the favorites of the account already say otherwise: they win. */
    private fun restoreFavorite(shopId: String, previous: Boolean) {
        val observed = favoriteIds?.contains(shopId)
        updateState { copy(shops = shops.map { if (it.id == shopId) it.copy(isFavorite = observed ?: previous) else it }) }
    }
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

private fun String.normalizedQuery(): String = trim().replace(Regex("\\s+"), " ")
