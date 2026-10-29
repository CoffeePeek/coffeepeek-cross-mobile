package com.coffeepeek.feature.shelf.data.backend

import com.coffeepeek.feature.shelf.domain.model.ShelfFailure
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.*

internal val shelfJson = Json { ignoreUnknownKeys = true }

internal suspend inline fun <reified T> HttpResponse.shelfData(): T =
    shelfEnvelope<T>().data ?: throw ShelfFailure(ShelfIssue.InvalidResponse)

internal suspend inline fun <reified T> HttpResponse.shelfEnvelope(): ShelfEnvelope<T> {
    val raw = bodyAsText()
    if (!status.isSuccess()) throw shelfServerFailure(status.value)
    val payload = shelfJson.parseToJsonElement(raw) as? JsonObject ?: throw ShelfFailure(ShelfIssue.InvalidResponse)
    val success = (payload["isSuccess"] as? JsonPrimitive)?.booleanOrNull ?: throw ShelfFailure(ShelfIssue.InvalidResponse)
    if (!success) throw shelfServerFailure(400)
    return shelfJson.decodeFromString<ShelfEnvelope<T>>(raw)
}
