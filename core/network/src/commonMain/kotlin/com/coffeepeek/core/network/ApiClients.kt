package com.coffeepeek.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.serialization.kotlinx.json.json

/**
 * The four transports of one account, built to the contract the application's clients have always had:
 * client errors (4xx) are ordinary responses, server errors (5xx) fail with [ServiceUnavailableException].
 *
 * - [plainClient]: no credentials; refresh, sign-in and public calls.
 * - [client]: the API with the account's credentials and an HTTP cache.
 * - [imageClient]: uncached photos with the same credentials; it shares the API's refresh rotation.
 * - [uploadClient]: presigned storage uploads; no base URL, no credentials.
 *
 * [refreshWith] builds the exchange of a refresh token over [plainClient]; credentials are read for every request.
 * Engines come from the platform unless the factories are replaced (the Sandbox and tests supply their own).
 */
class ApiClients(
    url: String,
    cacheFolderPath: String,
    private val debug: Boolean,
    credentials: SessionCredentials,
    refreshWith: (HttpClient) -> suspend (String) -> Result<SessionTokens>,
    requireHttps: Boolean = true,
    apiClientFactory: (HttpClientConfig<*>.() -> Unit) -> HttpClient = ::createApiEngineClient,
    uploadClientFactory: (HttpClientConfig<*>.() -> Unit) -> HttpClient = ::createUploadEngineClient,
) {
    /** Told of every API request that completes; infrastructure knows no analytics SDK. */
    var requestObserver: RequestObserver? = null
    private val baseUrl = normalizeBaseUrl(url)

    val plainClient: HttpClient = apiClientFactory {
        install(UserAgent) { agent = deviceUserAgent() }
        installServerErrorValidation()
        install(ContentNegotiation) { json(networkJson) }
        defaultRequest { url(baseUrl) }
    }.also { intercept(it) }

    val uploadClient: HttpClient = uploadClientFactory {
        installServerErrorValidation()
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 120_000
        }
    }.also { intercept(it) }

    private val exchange = refreshWith(plainClient)
    private val session = object : BearerSession, SessionCredentials by credentials {
        override suspend fun refresh(refreshToken: String): Result<SessionTokens> = exchange(refreshToken)
    }
    private val coordinator = SessionRefreshCoordinator(session)
    private val markAuthRetry: (io.ktor.client.statement.HttpResponse) -> Unit =
        { it.call.request.attributes.getOrNull(logicalRequestKey)?.authRetried = true }

    /** Manual photo ETag/304 handling must never pass through Ktor's HttpCache. */
    val imageClient: HttpClient = apiClientFactory {
        installServerErrorValidation()
        // A stalled photo connection is cut quickly; the photo transport makes one more attempt at once.
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
            requestTimeoutMillis = 60_000
        }
        defaultRequest { url(baseUrl) }
        configureBearerAuthentication(baseUrl, session, coordinator, requireHttps, markAuthRetry)
    }.also { it.readCurrentSessionForRequests(); intercept(it) }

    val client: HttpClient = apiClientFactory {
        installServerErrorValidation()
        install(ContentNegotiation) { json(networkJson) }
        defaultRequest { url(baseUrl) }
        configureBearerAuthentication(baseUrl, session, coordinator, requireHttps, markAuthRetry)
        install(HttpCache) {
            createHttpCacheStorage(cacheFolderPath)?.let(::publicStorage)
        }
    }.also { it.readCurrentSessionForRequests(); intercept(it) }

    private fun intercept(httpClient: HttpClient) {
        if (debug) {
            httpClient.plugin(HttpSend).intercept { request ->
                httpDebugLog(with(CurlInterceptor) { request.asCurlString() })
                execute(request).also { call -> httpDebugLog("CURL ${call.response.status.value}") }
            }
        }
        httpClient.observeRequests(baseUrl) { requestObserver }
    }
}

internal fun normalizeBaseUrl(url: String): String = url.trim().trimEnd('/')
