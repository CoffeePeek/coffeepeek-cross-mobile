package com.coffeepeek.feature.roaster.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.roaster.data.backend.RoasterBackend
import com.coffeepeek.feature.roaster.data.backend.RoasterSummaryDto
import com.coffeepeek.feature.roaster.data.mapper.RoasterFileUrlResolver
import com.coffeepeek.feature.roaster.data.mapper.toDomain
import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.domain.model.RoasterSummary
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Persistent text slot for the catalog cache; the app backs it with its settings store. */
interface RoasterCatalogStore {
    suspend fun read(): String?
    suspend fun write(value: String)
}

/**
 * The application owns/configures the client and the store. The store scopes the cache (normally per API host,
 * under the legacy `roaster_catalog_v1:<host>` key); the persisted JSON is shape-compatible with the legacy cache.
 */
fun createRoasterRepository(
    client: HttpClient,
    fileBaseUrl: String,
    store: RoasterCatalogStore,
    nowMs: () -> Long,
): RoasterRepository = RemoteRoasterRepository(RoasterBackend(client), RoasterFileUrlResolver(fileBaseUrl), store, nowMs)

private val cacheJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private const val CATALOG_TTL_MS = 24 * 60 * 60 * 1_000L

@Serializable
private data class CachedCatalog(val savedAtMs: Long, val items: List<RoasterSummaryDto>)

private class RemoteRoasterRepository(
    private val backend: RoasterBackend,
    private val files: RoasterFileUrlResolver,
    private val store: RoasterCatalogStore,
    private val nowMs: () -> Long,
) : RoasterRepository {
    private val mutex = Mutex()
    private var memory: List<RoasterSummary>? = null

    // One request at a time; a loaded session stays stable, saved data is used while fresh or when offline.
    override suspend fun getRoasters(): Result<List<RoasterSummary>> = mutex.withLock {
        memory?.let { return@withLock Result.success(it) }
        val saved = try {
            store.read()?.let { runCatching { cacheJson.decodeFromString<CachedCatalog>(it) }.getOrNull() }
        } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { null }
        try {
            val items = if (saved != null && nowMs() - saved.savedAtMs in 0 until CATALOG_TTL_MS) saved.items else {
                val loaded = backend.list()
                currentCoroutineContext().ensureActive()
                try { store.write(cacheJson.encodeToString(CachedCatalog(nowMs(), loaded))) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* A cache write failure must not hide loaded public data. */ }
                loaded
            }
            Result.success(remember(items))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { if (saved != null) Result.success(remember(saved.items)) else Result.failure(error) }
    }

    private fun remember(items: List<RoasterSummaryDto>): List<RoasterSummary> {
        val knownSlugs = mutableSetOf<String>()
        return items.map { it.toDomain(files) }
            .filter { item -> item.address?.slug?.let(knownSlugs::add) ?: true }
            .also { memory = it }
    }

    override suspend fun getRoaster(slug: String): Result<RoasterDetails> = requestResult {
        backend.detail(slug).toDomain(files)
    }
}
