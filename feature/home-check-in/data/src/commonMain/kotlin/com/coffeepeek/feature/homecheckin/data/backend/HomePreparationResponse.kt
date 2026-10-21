package com.coffeepeek.feature.homecheckin.data.backend

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationFailure
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.*

private val responseJson = Json { ignoreUnknownKeys = true }

internal suspend inline fun <reified T> HttpResponse.homeData(): T =
    homeEnvelope<T>().data ?: throw HomePreparationFailure(HomePreparationIssue.InvalidResponse)

internal suspend inline fun <reified T> HttpResponse.homeEnvelope(): HomeEnvelope<T> {
    val raw = bodyAsText()
    if (!status.isSuccess()) throw homeServerFailure(status.value, raw)
    val payload = responseJson.parseToJsonElement(raw) as? JsonObject
        ?: throw HomePreparationFailure(HomePreparationIssue.InvalidResponse)
    val success = (payload["isSuccess"] as? JsonPrimitive)?.booleanOrNull
        ?: throw HomePreparationFailure(HomePreparationIssue.InvalidResponse)
    if (!success) throw homeServerFailure(400, raw)
    return responseJson.decodeFromString<HomeEnvelope<T>>(raw)
}
