@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.feature.attribution.data

import com.coffeepeek.feature.attribution.domain.*
import platform.Foundation.*

internal actual fun platformStore(directory: String): AttributionStore = IosAttributionStore(directory)

private class IosAttributionStore(private val directory: String) : AttributionStore {
    private val path = "$directory/acquisition-v1.json"
    override suspend fun load() = decodeAttribution(NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null))
    override suspend fun save(state: AttributionLocalState) {
        check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null))
        val folder = requireNotNull(NSURL.fileURLWithPath(directory))
        check(folder.setResourceValue(true, NSURLIsExcludedFromBackupKey, null))
        val value = encodeAttribution(state) as NSString
        check(value.writeToFile(path, true, NSUTF8StringEncoding, null))
    }
}

internal actual fun platformInstallChannel(context: Any?): String = "unknown"
internal actual fun platformInstallSource(context: Any?) = AttributionInstallSource { null }
