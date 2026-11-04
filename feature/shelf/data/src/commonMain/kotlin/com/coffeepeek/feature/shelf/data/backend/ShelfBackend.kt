package com.coffeepeek.feature.shelf.data.backend

import com.coffeepeek.feature.shelf.domain.model.ShelfFailure
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement

private const val ROOT = "/api/v1/shelf"
private val bagId = Regex("^[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")

/** Routes of `coffeepeek-backend/docs/api/shelf-client.md`; the client carries the owner's session. */
internal class ShelfBackend(private val client: HttpClient) {
    suspend fun shelf(): List<ShelfBagDto> = client.get(ROOT).shelfData()

    suspend fun archive(page: Int, pageSize: Int, query: String?): ShelfArchiveDto = client.get("$ROOT/archive") {
        parameter("pageNumber", page); parameter("pageSize", pageSize)
        query?.let { parameter("query", it) }
    }.shelfData()

    suspend fun create(body: ShelfBagWriteDto): ShelfBagDto = client.post("$ROOT/bags") { json(body) }.shelfData()
    suspend fun update(id: String, body: ShelfBagWriteDto): ShelfBagDto = client.put(bag(id)) { json(body) }.shelfData()
    suspend fun order(ids: List<String>): List<ShelfBagDto> = client.put("$ROOT/order") { json(ShelfOrderDto(ids)) }.shelfData()
    suspend fun finish(id: String, body: ShelfFinishDto): ShelfBagDto = client.post("${bag(id)}/finish") { json(body) }.shelfData()
    suspend fun restore(id: String, body: ShelfRestoreDto): ShelfBagDto = client.post("${bag(id)}/restore") { json(body) }.shelfData()

    suspend fun delete(id: String) {
        client.delete(bag(id)).shelfEnvelope<JsonElement>()
    }

    private fun bag(id: String): String {
        if (!id.matches(bagId)) throw ShelfFailure(ShelfIssue.InvalidInput)
        return "$ROOT/bags/$id"
    }

    private inline fun <reified T : Any> HttpRequestBuilder.json(body: T) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}
