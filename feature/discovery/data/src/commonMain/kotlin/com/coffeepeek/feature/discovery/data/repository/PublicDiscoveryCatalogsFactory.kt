package com.coffeepeek.feature.discovery.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.discovery.api.PublicCatalogOption
import com.coffeepeek.feature.discovery.api.PublicDiscoveryCatalogs
import com.coffeepeek.feature.discovery.data.backend.DiscoveryBackend
import io.ktor.client.HttpClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Reuses the catalog endpoints of the search; a failure is retried by the next call, a success is kept. */
fun createPublicDiscoveryCatalogs(client: HttpClient): PublicDiscoveryCatalogs = object : PublicDiscoveryCatalogs {
    private val backend = DiscoveryBackend(client)
    private val lock = Mutex()
    private var cities: List<PublicCatalogOption>? = null
    private var brewMethods: List<PublicCatalogOption>? = null

    override suspend fun cities() = lock.withLock {
        cities?.let { return@withLock Result.success(it) }
        requestResult { backend.cities().getOrThrow().map { PublicCatalogOption(it.address.slug, it.name) } }
            .also { result -> result.getOrNull()?.let { cities = it } }
    }

    override suspend fun brewMethods() = lock.withLock {
        brewMethods?.let { return@withLock Result.success(it) }
        requestResult { backend.brewMethods().getOrThrow().map { PublicCatalogOption(it.key, it.name.orEmpty()) } }
            .also { result -> result.getOrNull()?.let { brewMethods = it } }
    }
}
