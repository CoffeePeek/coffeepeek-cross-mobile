package com.coffeepeek.api

import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.FileStorage
import java.io.File

internal actual fun deviceUserAgent(): String = formatDeviceUserAgent(
    androidDeviceModel(Build.MANUFACTURER.orEmpty(), Build.MODEL.orEmpty()),
    "Android ${Build.VERSION.RELEASE}",
)

internal actual fun createClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(OkHttp) {
        block()
    }
}

internal actual fun createUploadClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(OkHttp) {
        block()
    }
}

internal actual fun createHttpCacheStorage(cacheFolderPath: String): CacheStorage? =
    FileStorage(File(cacheFolderPath))
