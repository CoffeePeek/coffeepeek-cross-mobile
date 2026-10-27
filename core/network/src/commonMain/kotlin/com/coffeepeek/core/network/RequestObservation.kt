package com.coffeepeek.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.plugins.ResponseException
import io.ktor.http.Url
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlin.time.TimeSource

/** Application-supplied observer. Infrastructure exposes no SDK or business analytics dependency. */
fun interface RequestObserver {
    fun begin(category: String, method: String): RequestObservation?
}
fun interface RequestObservation {
    fun complete(status: Int?, failure: Throwable?, durationMs: Long, authRetried: Boolean)
}

internal class LogicalRequest { var authRetried = false }
internal val logicalRequestKey = AttributeKey<LogicalRequest>("coffeepeek.logicalRequest")

internal fun HttpClient.observeRequests(baseUrl: String, observer: () -> RequestObserver?) {
    val origin = Url(baseUrl)
    requestPipeline.intercept(HttpRequestPipeline.State) {
        val request = context
        val url = request.url.build()
        val method = request.method.value
        val path = url.encodedPath.lowercase()
        val isApi = url.protocol == origin.protocol && url.host == origin.host && url.port == origin.port
        // Refresh is part of its parent's logical operation, and storage uploads are not API requests.
        if (!isApi || method !in setOf("GET", "POST", "PUT", "PATCH", "DELETE") || method == "PUT" && path.trimEnd('/') == "/api/tokens") {
            proceed()
            return@intercept
        }
        val observation = runCatching { observer()?.begin(apiCategory(path), method) }.getOrNull()
        if (observation == null) { proceed(); return@intercept }
        val logical = LogicalRequest()
        request.attributes.put(logicalRequestKey, logical)
        val start = TimeSource.Monotonic.markNow()
        try {
            proceed()
            val call = subject as? HttpClientCall
            runCatching { observation.complete(call?.response?.status?.value, null, start.elapsedNow().inWholeMilliseconds.coerceAtLeast(0), logical.authRetried) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val status = (error as? ResponseException)?.response?.status?.value ?: (error as? HttpStatusFailure)?.statusCode
            runCatching { observation.complete(status, error, start.elapsedNow().inWholeMilliseconds.coerceAtLeast(0), logical.authRetried) }
            throw error
        }
    }
}

internal fun apiCategory(path: String): String = when {
    "/tokens" in path || "/password" in path -> "authentication"
    "/favorite" in path -> "favorites"
    "/check-in" in path || "/home-check" in path -> "check_ins"
    "/photo" in path || "/upload" in path -> "photos"
    "/coffees" in path -> "coffees"
    "/roaster" in path -> "roasters"
    "/coffeeshop" in path || "/shops" in path -> "shops"
    "/feed" in path -> "feed"
    "/catalog" in path -> "catalog"
    "/users" in path || "/profile" in path -> "profile"
    else -> "other"
}
