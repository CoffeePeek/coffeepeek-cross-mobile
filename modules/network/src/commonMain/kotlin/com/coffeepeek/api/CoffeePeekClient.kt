package com.coffeepeek.api

import com.coffeepeek.api.model.response.AuthResp
import com.coffeepeek.api.service.AuthService
import com.coffeepeek.api.utils.CurlInterceptor.asCurlString
import com.coffeepeek.api.utils.httpDebugLog
import com.coffeepeek.api.utils.JsonExt
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.serialization.kotlinx.json.json

internal expect fun createClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient

internal expect fun createUploadClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient

internal expect fun createHttpCacheStorage(cacheFolderPath: String): CacheStorage?

class CoffeePeekClient(
    url: String,
    cacheFolderPath: String,
    private val debug: Boolean,
    private val getToken: () -> AuthResp?,
    private val saveToken: (AuthResp?) -> Unit,
) {
    private val baseUrl = normalizeBaseUrl(url)

    private fun resolveTokens(): AuthResp? = getToken()

    private fun persistTokens(tokens: AuthResp?) {
        saveToken(tokens)
    }

    val plainClient: HttpClient = createClient {
        install(ContentNegotiation) { json(json = JsonExt.json) }
        defaultRequest { url(baseUrl) }
    }.also { intercept(it) }

    val uploadClient: HttpClient = createUploadClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 120_000
        }
    }.also { intercept(it) }

    private val tokenRefreshService = AuthService(plainClient, plainClient)

    val client: HttpClient = createClient {
        install(ContentNegotiation) { json(json = JsonExt.json) }
        defaultRequest { url(baseUrl) }

        configureSessionAuthentication(baseUrl, ::resolveTokens, ::persistTokens) {
            tokenRefreshService.refresh(it).getOrThrow()
        }

        install(HttpCache) {
            createHttpCacheStorage(cacheFolderPath)?.let(::publicStorage)
        }
    }.also { it.readCurrentSessionForRequests(); intercept(it) }

    val authService: AuthService by lazy { AuthService(client, plainClient) }

    private fun intercept(httpClient: HttpClient) {
        if (debug) {
            httpClient.plugin(HttpSend).intercept { request ->
                val message = request.asCurlString()
                httpDebugLog(message)
                execute(request).also { responseCall ->
                    httpDebugLog("CURL ${responseCall.response.status.value}")
                }
            }
        }
    }
}

internal fun normalizeBaseUrl(url: String): String = url.trim().trimEnd('/')
