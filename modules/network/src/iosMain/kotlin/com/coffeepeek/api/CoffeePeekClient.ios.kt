package com.coffeepeek.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.cache.storage.CacheStorage
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.valueForHTTPHeaderField

internal actual fun createClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin) {
        engine { configureRequest {
            if (valueForHTTPHeaderField("Cache-Control")?.contains("no-store", ignoreCase = true) == true) {
                setCachePolicy(NSURLRequestReloadIgnoringLocalCacheData)
            }
        } }
        block()
    }

internal actual fun createUploadClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin) { block() }

// Ktor's persistent FileStorage is JVM-only. The default in-memory cache is used on iOS;
// Protected no-store requests also bypass NSURLSession's response cache above.
internal actual fun createHttpCacheStorage(cacheFolderPath: String): CacheStorage? = null
