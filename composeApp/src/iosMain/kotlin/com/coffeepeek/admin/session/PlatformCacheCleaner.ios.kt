@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.session

import platform.Foundation.NSFileManager

internal actual fun clearPlatformCaches(
    httpCacheFolderPath: String,
    appCacheRootPath: String,
) {
    val fileManager = NSFileManager.defaultManager
    fileManager.removeItemAtPath(httpCacheFolderPath, null)
    fileManager.createDirectoryAtPath(
        path = httpCacheFolderPath,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )

    val entries = fileManager.contentsOfDirectoryAtPath(appCacheRootPath, null)
        ?.filterIsInstance<String>()
        .orEmpty()
    entries
        .filter { name ->
            name.equals("kamel", ignoreCase = true) ||
                name.contains("image", ignoreCase = true)
        }
        .forEach { name ->
            fileManager.removeItemAtPath("$appCacheRootPath/$name", null)
        }
}
