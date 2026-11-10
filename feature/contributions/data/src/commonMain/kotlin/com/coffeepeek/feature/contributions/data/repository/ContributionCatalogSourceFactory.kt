package com.coffeepeek.feature.contributions.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.contributions.data.backend.CatalogBackend
import com.coffeepeek.feature.contributions.data.mapper.toOption
import com.coffeepeek.feature.contributions.data.mapper.toSnapshot
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.Catalogs
import com.coffeepeek.feature.contributions.domain.model.DrinkOption
import com.coffeepeek.feature.contributions.domain.model.ShopSnapshot
import com.coffeepeek.feature.contributions.domain.repository.ContributionCatalogSource
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

/**
 * [roasters] is the public roaster catalog the application already owns, so the forms and the roaster screens never disagree.
 * The catalogs and the drinks are kept for the lifetime of the source after a successful read; a failure is retried by the next call.
 */
fun createContributionCatalogSource(
    client: HttpClient,
    roasters: suspend () -> Result<List<CatalogOption>>,
): ContributionCatalogSource = CatalogSource(CatalogBackend(client), roasters)

private class CatalogSource(
    private val backend: CatalogBackend,
    private val roasters: suspend () -> Result<List<CatalogOption>>,
) : ContributionCatalogSource {
    private val lock = Mutex()
    // The load runs here, not in the first caller: a caller that stops waiting must not cancel it for the others.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cachedCatalogs: Catalogs? = null
    private var catalogsLoad: Deferred<Result<Catalogs>>? = null
    private var cachedDrinks: List<DrinkOption>? = null
    private var cachedOrder: List<String>? = null

    override suspend fun catalogs(): Result<Catalogs> {
        cachedCatalogs?.let { return Result.success(it) }
        val load = lock.withLock {
            cachedCatalogs?.let { return Result.success(it) }
            // A finished load is never reused: a success is already in the cache, a failure must be retried.
            catalogsLoad?.takeIf { !it.isCompleted } ?: scope.async { fetchCatalogs().also { keep(it) } }.also { catalogsLoad = it }
        }
        return load.await().also {
            lock.withLock { if (catalogsLoad === load) catalogsLoad = null }
        }
    }

    // Recorded by the load itself, so a caller arriving right after it finished finds the success instead of starting another read.
    private suspend fun keep(result: Result<Catalogs>) {
        result.getOrNull()?.let { catalogs -> lock.withLock { cachedCatalogs = catalogs } }
    }

    private suspend fun fetchCatalogs(): Result<Catalogs> = requestResult {
        coroutineScope {
            val cities = async { backend.cities().getOrThrow() }
            val beans = async { backend.beans().getOrThrow() }
            val equipment = async { backend.equipment().getOrThrow() }
            val brewMethods = async { backend.brewMethods().getOrThrow() }
            val tags = async { backend.shopTags().getOrThrow() }
            val roasterOptions = async { roasters().getOrThrow() }
            Catalogs(
                cities = cities.await().map { CatalogOption(it.address.slug.orEmpty(), it.name) },
                beans = beans.await().map { it.toOption() },
                equipment = equipment.await().map { it.toOption() },
                roasters = roasterOptions.await(),
                brewMethods = brewMethods.await().map { it.toOption() },
                tags = tags.await().map { it.toOption() },
            )
        }
    }

    override suspend fun drinks(): Result<List<DrinkOption>> = requestResult {
        lock.withLock {
            cachedDrinks ?: backend.drinks().getOrThrow().sortedBy { it.sortOrder }.also { sorted ->
                cachedOrder = sorted.map { it.slug }
                cachedDrinks = sorted.map { it.toOption() }
            }.let { cachedDrinks!! }
        }
    }

    override suspend fun shop(slug: String): Result<ShopSnapshot> = requestResult {
        coroutineScope {
            val details = async { backend.shop(slug).getOrThrow() }
            // The menu is ordered like the catalog when the catalog is available; a failed catalog read does not fail the shop.
            val order = async {
                try {
                    drinks().getOrNull()?.let { cachedOrder }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                }
            }
            details.await().toSnapshot(slug, order.await())
        }
    }
}
