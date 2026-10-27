package com.coffeepeek.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.cache.storage.CacheStorage

/** The platform engine for the API transports. */
internal expect fun createApiEngineClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient

/** The platform engine for presigned uploads. */
internal expect fun createUploadEngineClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient

/** A persistent HTTP cache where the platform has one; null keeps Ktor's in-memory cache. */
internal expect fun createHttpCacheStorage(cacheFolderPath: String): CacheStorage?
