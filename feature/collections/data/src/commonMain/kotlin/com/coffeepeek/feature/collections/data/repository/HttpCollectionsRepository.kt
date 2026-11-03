package com.coffeepeek.feature.collections.data.repository

import com.coffeepeek.feature.collections.data.backend.*
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.*
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

internal class HttpCollectionsRepository(private val client: HttpClient, private val publicClient: HttpClient,
    private val apiBaseUrl: String, private val account: CollectionsAccount,
    private val pinRequest: (HttpRequestBuilder, String) -> Unit) : CollectionsRepository {
    override suspend fun favorites(): Result<List<CollectionPlace>> = owned { owner ->
        envelope<List<CollectionPlaceDto>>(client.get(api("/api/v1/favorites")) { pinRequest(this, owner) })
            .mapNotNull(::place).distinctBy(CollectionPlace::key).let(CollectionRules::orderedPlaces)
    }

    override suspend fun create(title: String, places: List<CollectionPlace>): Result<SharedCollection> = owned { owner ->
        require(CollectionRules.validTitle(title) && CollectionRules.validSelection(places))
        val request = CreateCollectionDto(title.trim(), places.map { CollectionSelectionDto(wireKind(it.kind), it.slug) })
        collection(envelope(client.post(api(path)) { pinRequest(this, owner); contentType(ContentType.Application.Json); setBody(request) }))
    }

    override suspend fun read(id: String): Result<SharedCollection> = request {
        collection(envelope(publicClient.get(api("$path/${validId(id)}"))))
    }

    override suspend fun mine(): Result<List<CollectionSummary>> = owned { owner ->
        envelope<List<CollectionSummaryDto>>(client.get(api(path)) { pinRequest(this, owner) }).map {
            CollectionSummary(it.id, it.title, publicUrl(it.id, it.canonicalPath), it.itemCount)
        }
    }

    override suspend fun revoke(id: String): Result<Unit> = owned { owner ->
        val response = client.delete(api("$path/${validId(id)}")) { pinRequest(this, owner) }
        check(response.status.isSuccess() || response.status == HttpStatusCode.NotFound)
    }

    override suspend fun photo(url: String): ByteArray? {
        val parsed = runCatching { Url(url) }.getOrNull() ?: return null
        if (parsed.protocol != URLProtocol.HTTPS || parsed.user != null || parsed.password != null) return null
        return try {
            withTimeoutOrNull(8_000) {
                publicClient.prepareGet(parsed).execute { response ->
                    if (!response.status.isSuccess() || response.contentLength()?.let { it > 2_000_000 } == true) null
                    else response.bodyAsChannel().readRemaining(2_000_001L).readByteArray().takeIf { it.size <= 2_000_000 }
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
    }

    private suspend fun <T> owned(block: suspend (String) -> T): Result<T> = request {
        val owner = checkNotNull(account.loadId())
        if (owner != account.currentId()) throw CancellationException("Collection account changed")
        val value = block(owner)
        if (owner != account.currentId()) throw CancellationException("Collection account changed")
        value
    }

    private suspend inline fun <T> request(block: () -> T): Result<T> = try { Result.success(block()) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { Result.failure(error) }

    private suspend inline fun <reified T> envelope(response: HttpResponse): T {
        check(response.status.isSuccess()) { "Collection request failed: ${response.status.value}" }
        val body = response.body<CollectionEnvelope<T>>()
        check(body.isSuccess)
        return checkNotNull(body.data)
    }

    private fun collection(dto: CollectionDto): SharedCollection = SharedCollection(dto.id, dto.title,
        publicUrl(dto.id, dto.canonicalPath), CollectionRules.orderedPlaces(dto.items.mapNotNull(::place)))

    private fun place(dto: CollectionPlaceDto): CollectionPlace? {
        val kind = when (dto.kind) { "coffee_shop" -> CollectionPlaceKind.CoffeeShop; "roaster" -> CollectionPlaceKind.Roaster; else -> return null }
        val address = dto.address ?: return null
        if (address.slug.isBlank() || address.slug.any { it == '/' || it == '\\' || it.isISOControl() } || address.slug in listOf(".", "..")) return null
        val photo = dto.coverPhoto?.let { it.urls?.card ?: it.fullUrl }?.let(::photoUrl)
        return CollectionPlace(kind, address.slug, dto.name.ifBlank { address.slug }, photo, dto.cityName, dto.streetAddress,
            dto.rating?.takeIf { it.isFinite() && it > 0.0 && it <= 5.0 }, dto.checkInCount?.takeIf { it >= 0 })
    }

    private fun publicUrl(id: String, canonicalPath: String): String {
        require(canonicalPath == "/collections/${validId(id)}")
        return "https://coffeepeek.by$canonicalPath"
    }

    private fun photoUrl(raw: String): String? {
        val value = if (raw.startsWith("/") && !raw.startsWith("//")) apiBaseUrl.trimEnd('/') + raw else raw
        val url = runCatching { Url(value) }.getOrNull() ?: return null
        return value.takeIf { url.protocol == URLProtocol.HTTPS && url.user == null && url.password == null }
    }

    private fun validId(id: String): String {
        require(Regex("[0-9a-fA-F]{32}|[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}").matches(id))
        return id.replace("-", "").lowercase()
    }
    private fun api(path: String): String = apiBaseUrl.trimEnd('/') + path
    private fun wireKind(kind: CollectionPlaceKind): String = if (kind == CollectionPlaceKind.CoffeeShop) "coffee_shop" else "roaster"
    private companion object { const val path = "/api/v1/favorite-collections" }
}
