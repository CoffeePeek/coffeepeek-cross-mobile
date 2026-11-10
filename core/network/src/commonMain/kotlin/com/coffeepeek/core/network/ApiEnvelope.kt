@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.coffeepeek.core.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/** The server's response envelope; `data` is read as the type each call expects. */
@Serializable
data class ApiEnvelope<T>(
    @JsonNames("IsSuccess") val isSuccess: Boolean = false,
    @JsonNames("Message") val message: String? = null,
    @JsonNames("Data") val data: T? = null,
)

/** The message the server put into an error response, or null when the body is not an envelope or the message is blank. Cancellation passes through. */
suspend fun HttpResponse.errorMessage(): String? = try {
    body<ApiEnvelope<kotlinx.serialization.json.JsonElement>>().message?.takeIf(String::isNotBlank)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

/** The data of a successful envelope; a rejected one or a missing `data` fails with [failure], built from the server's message. */
suspend inline fun <reified T> HttpResponse.envelopeData(failure: (message: String?) -> Throwable): T {
    val envelope = body<ApiEnvelope<T>>()
    if (!envelope.isSuccess) throw failure(envelope.message?.takeIf(String::isNotBlank))
    return envelope.data ?: throw IllegalStateException("Missing response data")
}
