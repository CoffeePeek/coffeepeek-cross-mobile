package com.coffeepeek.feature.collections.data.repository

import com.coffeepeek.feature.collections.data.backend.*
import com.coffeepeek.feature.collections.data.mapper.*
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.io.readByteArray

internal class HttpSavedCollectionsRepository(private val client: HttpClient, private val publicClient: HttpClient,
    private val apiBaseUrl: String, private val account: CollectionsAccount,
    private val pinRequest: (HttpRequestBuilder, String) -> Unit) : SavedCollectionsRepository {
    private val coverRequests = Semaphore(4)
    private val defaultCreation = Mutex()
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun observeChanges() = changes.asSharedFlow()

    override suspend fun mine(target: SavedCollectionTarget?): Result<List<SavedCollectionSummary>> = owned { owner ->
        defaultCreation.lock()
        try { mineWithDefault(owner, target) }
        finally { defaultCreation.unlock() }
    }

    private suspend fun mineWithDefault(owner: String, target: SavedCollectionTarget?): List<SavedCollectionSummary> {
        val lists = readMine(owner, target)
        checkOwner(owner)
        if (lists.any { it.isDefault }) return lists
        if (ensureServerDefault(owner)) return readMine(owner, target)
        if (lists.any { it.isDefaultFavorites() }) return lists
        val result = create(SavedCollectionMetadata(DEFAULT_FAVORITES_TITLE))
        result.getOrNull()?.let { return listOf(it.summary) + lists }
        val error = checkNotNull(result.exceptionOrNull())
        if (error !is SavedCollectionConflict) throw error
        return readMine(owner, target).takeIf { current -> current.any { it.isDefaultFavorites() } } ?: throw error
    }

    private suspend fun ensureServerDefault(owner: String): Boolean {
        val response = client.put(api("$path/default")) {
            pinRequest(this, owner)
            expectSuccess = false
        }
        if (response.status == HttpStatusCode.NotFound) return false
        check(detail(response, true).summary.isDefault)
        checkOwner(owner)
        return true
    }

    private suspend fun readMine(owner: String, target: SavedCollectionTarget?): List<SavedCollectionSummary> {
        val response = client.get(api(path)) {
            pinRequest(this, owner)
            target?.let { parameter("kind", collectionWireKind(it.kind)); parameter("slug", validSlug(it.slug)) }
        }
        return envelope<List<SavedCollectionDto>>(response).map { it.toSavedSummary(true) }
    }

    override suspend fun owned(id: String): Result<SavedCollection> = owned { owner ->
        detail(client.get(api("$path/${collectionId(id)}")) { pinRequest(this, owner) }, true)
    }

    override suspend fun public(id: String): Result<SavedCollection> = request {
        detail(publicClient.get(api("$publicPath/${collectionId(id)}")), false)
    }

    override suspend fun create(metadata: SavedCollectionMetadata): Result<SavedCollection> = mutate { owner ->
        require(SavedCollectionRules.validMetadata(metadata))
        detail(client.post(api(path)) {
            pinRequest(this, owner)
            contentType(ContentType.Application.Json)
            setBody(metadata.toRequest(new = true))
        }, true)
    }

    override suspend fun update(id: String, metadata: SavedCollectionMetadata, revision: Long): Result<SavedCollection> = mutate { owner ->
        require(revision > 0 && SavedCollectionRules.validMetadata(metadata))
        detail(client.put(api("$path/${collectionId(id)}")) {
            pinRequest(this, owner)
            contentType(ContentType.Application.Json)
            setBody(metadata.toRequest(revision))
        }, true)
    }

    override suspend fun replaceItems(id: String, places: List<CollectionPlace>, revision: Long): Result<SavedCollection> = mutate { owner ->
        require(revision > 0 && places.map { it.key }.distinct().size == places.size)
        detail(client.put(api("$path/${collectionId(id)}/items")) {
            pinRequest(this, owner)
            contentType(ContentType.Application.Json)
            setBody(SavedItemsDto(places.toSavedItems(), revision))
        }, true)
    }

    override suspend fun memberships(target: SavedCollectionTarget, collectionIds: Set<String>): Result<List<SavedCollectionSummary>> = mutate { owner ->
        writeMemberships(owner, target, collectionIds)
    }

    override suspend fun changeMemberships(target: SavedCollectionTarget, added: Set<String>, removed: Set<String>): Result<List<SavedCollectionSummary>> = mutate { owner ->
        val additions = added.map(::collectionId).toSet()
        val removals = removed.map(::collectionId).toSet()
        require(additions.intersect(removals).isEmpty())
        val current = readMine(owner, target).filter { it.containsItem }.map { collectionId(it.id) }.toSet()
        checkOwner(owner)
        writeMemberships(owner, target, (current + additions) - removals, additions, removals)
    }

    private suspend fun writeMemberships(owner: String, target: SavedCollectionTarget, ids: Set<String>,
        added: Set<String>? = null, removed: Set<String>? = null): List<SavedCollectionSummary> {
        val response = client.put(api("$path/membership/${collectionWireKind(target.kind)}/${validSlug(target.slug)}")) {
            pinRequest(this, owner)
            contentType(ContentType.Application.Json)
            setBody(SavedMembershipDto(ids.map(::collectionBodyId), added?.map(::collectionBodyId), removed?.map(::collectionBodyId)))
        }
        return envelope<List<SavedCollectionDto>>(response).map { it.toSavedSummary(true) }
    }

    override suspend fun delete(id: String): Result<Unit> = mutate { owner ->
        success(client.delete(api("$path/${collectionId(id)}")) { pinRequest(this, owner) })
    }

    override suspend fun featuredId(): Result<String?> = owned { owner ->
        envelope<SavedFeaturedDto>(client.get(api("$path/featured")) { pinRequest(this, owner) }).collectionId
    }

    override suspend fun setFeatured(id: String?): Result<Unit> = mutate { owner ->
        success(client.put(api("$path/featured")) {
            pinRequest(this, owner)
            contentType(ContentType.Application.Json)
            setBody(SavedFeaturedDto(id?.let(::collectionBodyId)))
        })
    }

    override suspend fun featured(profileSlug: String): Result<SavedCollection?> = request {
        val response = publicClient.get(api("/api/v1/profiles/${validSlug(profileSlug)}/collection"))
        success(response)
        val body = response.body<CollectionEnvelope<SavedCollectionDto>>()
        check(body.isSuccess)
        body.data?.toSavedCollection(false, apiBaseUrl)
    }

    override suspend fun uploadCover(input: CollectionCoverInput): Result<CollectionCoverUpload> = owned { owner ->
        require(SavedCollectionRules.validCover(input))
        withTimeout(20_000) {
            val response = client.post(api("/api/Photos/collection-cover")) {
                pinRequest(this, owner)
                contentType(ContentType.Application.Json)
                setBody(listOf(SavedCoverReservationDto(input.fileName, input.mimeType, input.bytes.size.toLong())))
            }
            val target = envelope<List<SavedCoverReservationResponse>>(response).single()
            checkOwner(owner)
            check(target.storageKey.startsWith("collection-covers/"))
            upload(input, target)
            checkOwner(owner)
            CollectionCoverUpload(input.fileName, input.mimeType, target.storageKey, input.bytes.size.toLong())
        }
    }

    override suspend fun cover(collectionId: String, cover: SavedCollectionCover): Result<ByteArray> = withTimeout(8_000) {
        if (cover.authenticated) owned { owner -> coverBytes(collectionId, cover, owner) }
        else request { coverBytes(collectionId, cover, null) }
    }

    private suspend fun upload(input: CollectionCoverInput, target: SavedCoverReservationResponse) {
        val url = Url(target.uploadUrl)
        require(url.protocol == URLProtocol.HTTPS && url.user == null && url.password == null)
        success(publicClient.put(url) {
            contentType(ContentType.parse(input.mimeType))
            header("x-amz-tagging", "is_permanent=False")
            setBody(input.bytes)
        })
    }

    private suspend fun coverBytes(id: String, cover: SavedCollectionCover, owner: String?): ByteArray = coverRequests.withPermit {
        if (owner != null) checkOwner(owner)
        fetchCover(id, cover, owner)
    }

    private suspend fun fetchCover(id: String, cover: SavedCollectionCover, owner: String?): ByteArray {
        val base = if (owner != null) path else publicPath
        val loader = if (owner != null) client else publicClient
        return loader.prepareGet(api("$base/${collectionId(id)}/cover/${collectionId(cover.id)}")) {
            if (owner != null) pinRequest(this, owner)
        }.execute { response ->
            success(response)
            check(response.contentLength()?.let { it <= SavedCollectionRules.maxCoverBytes } != false)
            val bytes = response.bodyAsChannel().readRemaining(SavedCollectionRules.maxCoverBytes.toLong() + 1).readByteArray()
            if (bytes.size > SavedCollectionRules.maxCoverBytes || owner != null && owner != account.currentId()) {
                bytes.fill(0)
                if (owner != null) checkOwner(owner)
                error("Cover exceeds size limit")
            }
            bytes
        }
    }

    private suspend fun detail(response: HttpResponse, owned: Boolean): SavedCollection =
        envelope<SavedCollectionDto>(response).toSavedCollection(owned, apiBaseUrl)

    private suspend fun <T> owned(block: suspend (String) -> T): Result<T> = request {
        val owner = checkNotNull(account.loadId())
        checkOwner(owner)
        val value = block(owner)
        if (owner != account.currentId()) (value as? ByteArray)?.fill(0)
        checkOwner(owner)
        value
    }

    private suspend fun <T> mutate(block: suspend (String) -> T): Result<T> =
        owned(block).onSuccess { changes.tryEmit(Unit) }

    private fun checkOwner(owner: String) {
        if (owner != account.currentId()) throw CancellationException("Collection account changed")
    }

    private suspend inline fun <T> request(block: () -> T): Result<T> = try { Result.success(block()) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: ResponseException) {
        Result.failure(if (error.response.status == HttpStatusCode.Conflict) SavedCollectionConflict() else error)
    }
    catch (error: Exception) { Result.failure(error) }

    private suspend inline fun <reified T> envelope(response: HttpResponse): T {
        success(response)
        val body = response.body<CollectionEnvelope<T>>()
        check(body.isSuccess)
        return checkNotNull(body.data)
    }

    private fun success(response: HttpResponse) {
        if (response.status == HttpStatusCode.Conflict) throw SavedCollectionConflict()
        check(response.status.isSuccess()) { "Collection request failed: ${response.status.value}" }
    }

    private fun validSlug(slug: String): String {
        require(slug.isNotBlank() && slug !in listOf(".", "..") && slug.none { it in "/\\?#%" || it.isISOControl() })
        return slug
    }

    private fun api(relative: String) = apiBaseUrl.trimEnd('/') + relative

    private companion object {
        const val path = "/api/v1/saved-collections"
        const val publicPath = "/api/v1/favorite-collections"
        const val DEFAULT_FAVORITES_TITLE = "Избранное"
    }
}

private fun SavedCollectionSummary.isDefaultFavorites(): Boolean =
    title == "Избранное"
