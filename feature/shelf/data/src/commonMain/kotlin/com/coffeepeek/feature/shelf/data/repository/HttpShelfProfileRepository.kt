package com.coffeepeek.feature.shelf.data.repository

import com.coffeepeek.feature.shelf.data.backend.*
import com.coffeepeek.feature.shelf.domain.model.*
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.domain.repository.PublicShelfSnapshot
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.*
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.io.readByteArray

internal class HttpShelfProfileRepository(
    private val client: HttpClient,
    private val publicClient: HttpClient,
    apiBaseUrl: String,
    private val account: ShelfAccount,
    private val pinRequest: (HttpRequestBuilder, String) -> Unit,
) : ShelfProfileRepository {
    private val base = Url(apiBaseUrl)
    private val permits = Semaphore(4)
    private val snapshots = MutableStateFlow(ShelfProfileCache())
    override val owners get() = account.owners

    init { require(base.host.isNotBlank() && base.protocol.name in setOf("http", "https")) }

    override fun cachedPublicShelf(slug: String): PublicShelfSnapshot? {
        val owner = account.owner
        snapshots.update { if (it.owner == owner) it else ShelfProfileCache(owner, sequence = it.sequence) }
        return snapshots.value.values[slug].takeIf { owner != null && owner == account.owner }
    }

    override suspend fun visibility(): Result<Boolean> = owned { owner ->
        client.get(api(VISIBILITY)) { pinRequest(this, owner); expectSuccess = false }
            .shelfData<ShelfProfileVisibilityDto>().isVisible
    }

    override suspend fun setVisibility(visible: Boolean): Result<Boolean> = owned { owner ->
        client.put(api(VISIBILITY)) {
            pinRequest(this, owner)
            expectSuccess = false
            contentType(ContentType.Application.Json)
            setBody(ShelfProfileVisibilityDto(visible))
        }.shelfData<ShelfProfileVisibilityDto>().isVisible
    }.onSuccess { snapshots.update { ShelfProfileCache(account.owner, sequence = it.sequence) } }

    override suspend fun publicShelf(slug: String): Result<PublicShelf?> {
        val owner = account.owner
        val request = beginPublicRead(owner, slug)
        return readPublicShelf(slug).onSuccess { value ->
            if (owner != null && owner == account.owner) snapshots.update { current ->
                if (current.owner != owner || current.latest[slug] != request) current
                else current.copy(values = (current.values + (slug to PublicShelfSnapshot(value))).toList().takeLast(8).toMap())
            }
        }
    }

    private fun beginPublicRead(owner: String?, slug: String): Int = snapshots.updateAndGet { previous ->
        val current = previous.takeIf { it.owner == owner } ?: ShelfProfileCache(owner, sequence = previous.sequence)
        val next = current.sequence + 1
        current.copy(sequence = next, latest = (current.latest + (slug to next)).toList().takeLast(8).toMap())
    }.sequence

    private suspend fun readPublicShelf(slug: String): Result<PublicShelf?> = shelfRequestResult {
        requireValidSlug(slug)
        val response = publicClient.get(api("/api/v1/profiles/$slug/shelf")) {
            expectSuccess = false
            header(HttpHeaders.CacheControl, "no-store")
        }.shelfEnvelope<PublicShelfDto>()
        response.data?.let { dto ->
            require(dto.totalCount >= dto.items.size && dto.items.map { it.id }.distinct().size == dto.items.size)
            PublicShelf(dto.items.map(::item), dto.totalCount)
        }
    }

    override suspend fun photo(url: String): Result<ByteArray> = withTimeout(8_000) {
        shelfRequestResult { permits.withPermit { photoBytes(validPhotoUrl(url)) } }
    }

    private suspend fun photoBytes(url: Url): ByteArray = publicClient.prepareGet(url) {
        expectSuccess = false
        header(HttpHeaders.CacheControl, "no-store")
    }.execute { response ->
        if (!response.status.isSuccess()) throw shelfServerFailure(response.status.value)
        check(response.contentLength()?.let { it <= MAX_PHOTO_BYTES } != false)
        val bytes = response.bodyAsChannel().readRemaining(MAX_PHOTO_BYTES.toLong() + 1).readByteArray()
        if (bytes.size > MAX_PHOTO_BYTES) {
            bytes.fill(0)
            throw ShelfFailure(ShelfIssue.InvalidResponse)
        }
        bytes
    }

    private fun item(dto: PublicShelfItemDto): PublicShelfItem {
        require(dto.id.matches(UUID))
        dto.coffeeSlug?.let(::requireValidSlug)
        val photo = dto.photoUrl?.let { path -> validPhotoUrl(path).also {
            require(it.encodedPath.contains("/bags/${dto.id}/photos/"))
        }.toString() }
        return PublicShelfItem(dto.id, dto.name, dto.roasterName, dto.coffeeSlug, photo)
    }

    private fun validPhotoUrl(raw: String): Url {
        val url = Url(if (raw.startsWith("/") && !raw.startsWith("//")) api(raw) else raw)
        require(url.protocol == base.protocol && url.host == base.host && url.port == base.port)
        require(url.user == null && url.password == null && url.parameters.isEmpty() && url.fragment.isEmpty())
        require(PHOTO_PATH.matches(url.encodedPath))
        requireValidSlug(url.encodedPath.split('/')[4].decodeURLPart())
        return url
    }

    private suspend fun <T> owned(block: suspend (String) -> T): Result<T> = shelfRequestResult {
        val owner = account.owner ?: throw ShelfFailure(ShelfIssue.SignedOut)
        coroutineScope {
            val operation = currentCoroutineContext().job
            val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                account.owners.first { it != owner }
                operation.cancel(CancellationException("Shelf profile account changed"))
            }
            try {
                if (account.owner != owner) throw CancellationException("Shelf profile account changed")
                block(owner).also { if (account.owner != owner) throw CancellationException("Shelf profile account changed") }
            } finally { watcher.cancel() }
        }
    }

    private fun api(path: String) = base.toString().trimEnd('/') + path

    private fun requireValidSlug(slug: String) {
        require(slug.isNotBlank() && slug !in listOf(".", "..") && slug.none { it in "/\\?#%" || it.isISOControl() })
    }

    private companion object {
        const val VISIBILITY = "/api/v1/shelf/profile-visibility"
        const val MAX_PHOTO_BYTES = 8 * 1024 * 1024
        const val UUID_PATTERN = "[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"
        val UUID = Regex("^$UUID_PATTERN$")
        val PHOTO_PATH = Regex("^/api/v1/profiles/[^/?#]+/shelf/bags/$UUID_PATTERN/photos/$UUID_PATTERN$")
    }
}

private data class ShelfProfileCache(val owner: String? = null, val values: Map<String, PublicShelfSnapshot> = emptyMap(),
    val sequence: Int = 0, val latest: Map<String, Int> = emptyMap())
