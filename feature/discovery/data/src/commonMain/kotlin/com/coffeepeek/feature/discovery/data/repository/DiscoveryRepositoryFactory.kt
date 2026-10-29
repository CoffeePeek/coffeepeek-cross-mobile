package com.coffeepeek.feature.discovery.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.discovery.data.backend.DiscoveryBackend
import com.coffeepeek.feature.discovery.data.backend.MapBoundsDto
import com.coffeepeek.feature.discovery.data.backend.ShopMapRequestDto
import com.coffeepeek.feature.discovery.data.backend.ShopSearchFiltersDto
import com.coffeepeek.feature.discovery.data.backend.ShopSearchRequestDto
import com.coffeepeek.feature.discovery.data.backend.OriginDto
import com.coffeepeek.feature.discovery.data.mapper.toCatalogEntry
import com.coffeepeek.feature.discovery.data.mapper.toContent
import com.coffeepeek.feature.discovery.data.mapper.toDomain
import com.coffeepeek.feature.discovery.domain.model.CatalogEntry
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCatalogs
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFailure
import com.coffeepeek.feature.discovery.domain.model.DiscoveryFilters
import com.coffeepeek.feature.discovery.domain.model.DiscoveryCriteria
import com.coffeepeek.feature.discovery.domain.model.FailureKind
import com.coffeepeek.feature.discovery.domain.model.MapBounds
import com.coffeepeek.feature.discovery.domain.model.MapContent
import com.coffeepeek.feature.discovery.domain.model.PriceLevel
import com.coffeepeek.feature.discovery.domain.model.ShopKind
import com.coffeepeek.feature.discovery.domain.model.Paged
import com.coffeepeek.feature.discovery.domain.model.ShopQuery
import com.coffeepeek.feature.discovery.domain.model.ShopSummary
import com.coffeepeek.feature.discovery.domain.repository.DiscoveryRepository
import com.coffeepeek.feature.discovery.domain.rules.searchText
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val SHOP_ZOOM = 22
private const val MAX_PAGE_SIZE = 100

/**
 * [client] is the authenticated API client. [favoriteIds] are the favorite shops of the account: search merges them with server favorite flags (a failing snapshot
 * counts as empty). [roasters] are the roasters for the filter list; the application owns that catalog (it is cached on disk), so this module does not read it.
 */
fun createDiscoveryRepository(
    client: HttpClient,
    favoriteIds: suspend () -> Set<String>,
    roasters: suspend () -> Result<List<CatalogEntry>>,
): DiscoveryRepository = RemoteDiscoveryRepository(DiscoveryBackend(client), favoriteIds, roasters)

private class RemoteDiscoveryRepository(
    private val backend: DiscoveryBackend,
    private val favoriteIds: suspend () -> Set<String>,
    private val roasters: suspend () -> Result<List<CatalogEntry>>,
) : DiscoveryRepository {
    // The catalogs are read once and kept; a load runs in this scope so that a caller that stops waiting does not cancel the others.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val catalogsMutex = Mutex()
    private var cachedCatalogs: DiscoveryCatalogs? = null
    private var catalogsLoad: Deferred<Result<DiscoveryCatalogs>>? = null

    override suspend fun search(query: ShopQuery): Result<Paged<ShopSummary>> = requestResult {
        if (query.page < 1 || query.pageSize !in 1..MAX_PAGE_SIZE) throw DiscoveryFailure(FailureKind.Other)
        val favorites = favorites()
        val origin = query.origin
        if (origin != null && !origin.isValid) throw DiscoveryFailure(FailureKind.Other)
        searchPage(query, origin?.let { OriginDto(it.latitude, it.longitude) }, favorites)
    }

    private suspend fun searchPage(query: ShopQuery, origin: OriginDto?, favorites: Set<String>): Paged<ShopSummary> {
        val filters = query.filters
        val dto = backend.search(
            ShopSearchRequestDto(
                filters = filters.toRequest(origin),
                sort = if (origin != null) "distance_asc" else null,
                page = query.page, pageSize = query.pageSize, q = searchText(query.text.orEmpty()),
            ),
        ).getOrThrow()
        return Paged(
            dto.items.map { it.toDomain(favorites, filters.kind, visitedAsked = filters.visitedOnly, newAsked = filters.newOnly) },
            dto.totalItems, dto.totalPages, dto.currentPage,
        )
    }

    override suspend fun mapContent(bounds: MapBounds, zoom: Float, criteria: DiscoveryCriteria): Result<MapContent> = requestResult {
        val query = criteria.searchQuery()
        val origin = query.origin?.let { OriginDto(it.latitude, it.longitude) }
        // One finer server level halves cluster cells and unfolds dense areas sooner.
        val request = ShopMapRequestDto(searchText(query.text.orEmpty()).orEmpty(), query.filters.toRequest(origin),
            MapBoundsDto(bounds.minLat, bounds.minLon, bounds.maxLat, bounds.maxLon), (zoom + 1f).toInt().coerceIn(0, SHOP_ZOOM))
        backend.map(request).getOrThrow().toContent(query.filters.kind)
    }

    override suspend fun catalogs(): Result<DiscoveryCatalogs> {
        cachedCatalogs?.let { return Result.success(it) }
        val load = catalogsMutex.withLock {
            cachedCatalogs?.let { return Result.success(it) }
            catalogsLoad ?: scope.async { fetchCatalogs() }.also { catalogsLoad = it }
        }
        return load.await().also { result ->
            catalogsMutex.withLock {
                if (catalogsLoad === load) catalogsLoad = null
                result.getOrNull()?.let { cachedCatalogs = it }
            }
        }
    }

    private suspend fun fetchCatalogs(): Result<DiscoveryCatalogs> = requestResult {
        coroutineScope {
            val cities = async { backend.cities().getOrThrow() }
            val beans = async { backend.beans().getOrThrow() }
            val equipment = async { backend.equipment().getOrThrow() }
            val brewMethods = async { backend.brewMethods().getOrThrow() }
            val tags = async { backend.shopTags().getOrThrow() }
            val roasters = async { roasters().getOrThrow() }
            DiscoveryCatalogs(
                cities = cities.await().map { it.toDomain() }, beans = beans.await().map { it.toCatalogEntry() }, equipment = equipment.await().map { it.toCatalogEntry() },
                roasters = roasters.await(), brewMethods = brewMethods.await().map { it.toCatalogEntry() }, tags = tags.await().map { it.toCatalogEntry() },
            )
        }
    }

    private suspend fun favorites(): Set<String> = try {
        favoriteIds()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptySet()
    }
}

private fun DiscoveryFilters.toRequest(origin: OriginDto?) = ShopSearchFiltersDto(
    origin = origin, radiusKm = if (origin != null) 3 else null, city = cityId,
    tags = tagIds.toList(), roasters = roasterIds.toList(), equipments = equipmentIds.toList(),
    beans = beanIds.toList(), brewMethods = brewMethodIds.toList(), isOpen = true.takeIf { openOnly },
    isNew = true.takeIf { newOnly }, visitedOnly = visitedOnly, favoritesOnly = favoritesOnly,
    minRating = minRating, priceRange = priceLevel.searchValue(), type = kind.searchValue(),
)

private fun ShopKind?.searchValue(): String? = when (this) {
    ShopKind.Specialty -> "specialty"
    ShopKind.CoffeeBar -> "coffee-bar"
    ShopKind.Cafe -> "cafe"
    null -> null
}

private fun PriceLevel?.searchValue(): String? = when (this) {
    PriceLevel.Cheap -> "cheap"
    PriceLevel.Moderate -> "moderate"
    // shortcut: the API has three price bands; keep legacy Luxury in expensive until the fourth UI band is retired.
    PriceLevel.Expensive, PriceLevel.Luxury -> "expensive"
    null -> null
}
