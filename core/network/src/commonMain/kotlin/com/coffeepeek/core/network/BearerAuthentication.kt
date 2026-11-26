package com.coffeepeek.core.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url

internal fun HttpClientConfig<*>.configureBearerAuthentication(
    baseUrl: String,
    session: BearerSession,
    coordinator: SessionRefreshCoordinator? = null,
    requireHttps: Boolean = true,
    onAuthRetry: (HttpResponse) -> Unit = {},
) {
    val origin = Url(baseUrl.trim())
    if (requireHttps) require(origin.protocol == URLProtocol.HTTPS) { "Bearer authentication requires HTTPS" }
    val rotation = coordinator ?: SessionRefreshCoordinator(session)
    install(createClientPlugin("CoffeePeekBearerAuthentication") {
        on(Send) { request ->
            val credentials = if (request.url.build().isSameOrigin(origin)) rotation.credentials() else null
            credentials?.let { request.setBearer(it.tokens) }
            val first = proceed(request)
            if (credentials == null || !first.response.isRejectionOf(origin, credentials.tokens)) return@on first
            val updated = rotation.refresh(credentials) ?: return@on first
            onAuthRetry(first.response)
            request.setBearer(updated)
            proceed(request)
        }
    })
}

private fun HttpResponse.isRejectionOf(origin: Url, tokens: SessionTokens): Boolean =
    status == HttpStatusCode.Unauthorized && call.request.url.isSameOrigin(origin) &&
        call.request.headers[HttpHeaders.Authorization] == "Bearer ${tokens.accessToken}"

private fun Url.isSameOrigin(other: Url) =
    protocol == other.protocol && host == other.host && port == other.port

private fun HttpRequestBuilder.setBearer(tokens: SessionTokens) {
    headers[HttpHeaders.Authorization] = "Bearer ${tokens.accessToken}"
}
