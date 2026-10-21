package com.coffeepeek.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.cache.storage.CacheStorage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.valueForHTTPHeaderField
import platform.UIKit.UIDevice
import platform.posix.getenv
import platform.posix.uname
import platform.posix.utsname

@OptIn(ExperimentalForeignApi::class)
internal actual fun deviceUserAgent(): String {
    val simulator = getenv("SIMULATOR_MODEL_IDENTIFIER")?.toKString()
    val identifier = simulator ?: memScoped {
        val info = alloc<utsname>()
        if (uname(info.ptr) == 0) info.machine.toKString() else UIDevice.currentDevice.model
    }
    val model = appleDeviceModel(identifier).let { if (simulator != null) "$it Simulator" else it }
    val device = UIDevice.currentDevice
    return formatDeviceUserAgent(model, "${device.systemName} ${device.systemVersion}")
}

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
