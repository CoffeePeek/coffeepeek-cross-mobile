package com.coffeepeek.feature.homecheckin.data.backend

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement

private const val PATH = "/api/v1/check-ins/home"

internal class HomePreparationBackend(private val client: HttpClient) {
    suspend fun create(input: HomeCreateDto): HomeDto =
        client.post(PATH) { contentType(ContentType.Application.Json); setBody(input) }.homeData()

    suspend fun update(id: String, input: HomeEditDto): HomeDto =
        client.put(entryPath(id)) { contentType(ContentType.Application.Json); setBody(input) }.homeData()

    suspend fun detail(id: String): HomeDto = client.get(entryPath(id)).homeData()

    suspend fun delete(id: String) {
        client.delete(entryPath(id)).homeEnvelope<JsonElement>()
    }

    suspend fun page(page: Int, pageSize: Int, from: String?, to: String?): HomePageDto =
        client.get(PATH) {
            parameter("pageNumber", page); parameter("pageSize", pageSize)
            from?.let { parameter("from", it) }; to?.let { parameter("to", it) }
        }.homeData()

    suspend fun calendar(from: String, to: String): List<HomeCalendarDto> =
        client.get("$PATH/calendar") { parameter("from", from); parameter("to", to) }.homeData()

    private fun entryPath(id: String): String {
        require(id.matches(Regex("^[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$"))) { "Invalid preparation ID" }
        return "$PATH/$id"
    }
}
