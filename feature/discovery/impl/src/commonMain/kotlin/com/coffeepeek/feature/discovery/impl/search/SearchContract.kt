package com.coffeepeek.feature.discovery.impl.search

import com.coffeepeek.feature.discovery.domain.model.CityEntry
import com.coffeepeek.feature.discovery.domain.model.Coordinates
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCatalogs
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCriteria
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.domain.rules.showsDiscovery
import com.coffeepeek.feature.discovery.domain.rules.visibleShops

/** A failed search. [serverMessage] is the server's own wording; null shows the default text. */
internal data class SearchError(val serverMessage: String?)

/** Which list of the catalogs a selection belongs to. */
internal enum class CatalogKind { Roaster, Bean, Equipment, BrewMethod, Tag }

internal data class SearchState(
    /** True until an account is known to be signed in: a guest is sent to sign in for visited, favorites and the favorite toggle. */
    val isGuest: Boolean = true,
    val catalogs: DiscoveryCatalogs = DiscoveryCatalogs(),
    val shops: List<ShopSummary> = emptyList(),
    /** Shops whose favorite is being changed; at most one change per shop at a time. */
    val favoriteUpdates: Set<String> = emptySet(),
    /** The first search starts after the catalogs and the city are resolved; until then the screen stays loading so no empty state flashes. */
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: SearchError? = null,
    val query: String = "",
    val searchActive: Boolean = false,
    val filters: DiscoveryFilters = DiscoveryFilters(),
    val showFilters: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasMore: Boolean = false,
    /** A position is known, so «Рядом» can be offered. */
    val canUseNearby: Boolean = false,
    /** Where the user is, for the distance on a card when the server sends none. */
    val userLocation: Coordinates? = null,
) {
    fun showsShopsFirst(hasRoasterResults: Boolean?): Boolean =
        query.isNotBlank() && hasRoasterResults == false && visibleShops.isNotEmpty()

    val criteria: DiscoveryCriteria get() = DiscoveryCriteria(query, filters, userLocation)
    val activeFilterCount: Int get() = filters.activeCount
    val showsDiscovery: Boolean get() = showsDiscovery(query, searchActive, filters)
    val visibleShops: List<ShopSummary> get() = visibleShops(shops, filters)
    val selectedCity: CityEntry? get() = catalogs.cities.firstOrNull { it.id == filters.cityId }
}

internal sealed interface SearchAction {
    data class Query(val text: String) : SearchAction
    data class PresentationChanged(val active: Boolean) : SearchAction
    data object ActivateSearch : SearchAction
    data object CancelSearch : SearchAction
    data object ToggleFilters : SearchAction
    data object CloseFilters : SearchAction
    data class ApplyFilters(val filters: DiscoveryFilters) : SearchAction

    data class SetKind(val kind: ShopKind?) : SearchAction
    data object ToggleOpen : SearchAction
    data object ToggleNew : SearchAction
    data object ToggleVisited : SearchAction
    data object ToggleFavorites : SearchAction
    data object ToggleNearby : SearchAction
    /** The location permission was granted or refused by the screen's effect. */
    data object LocationGranted : SearchAction
    data object LocationDenied : SearchAction
    data class SetPrice(val level: PriceLevel?) : SearchAction
    data class SetMinRating(val rating: Double?) : SearchAction
    data class ToggleCatalog(val kind: CatalogKind, val id: String) : SearchAction
    data object ClearFilters : SearchAction

    data object LoadMore : SearchAction
    data object Refresh : SearchAction
    data class ToggleFavorite(val shopId: String) : SearchAction
    data class OpenShop(val shopId: String) : SearchAction
}

internal sealed interface SearchEvent {
    data class OpenShop(val shopId: String) : SearchEvent
    data object SignInRequired : SearchEvent
}
