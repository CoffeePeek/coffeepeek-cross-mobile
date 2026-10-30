package com.coffeepeek.admin.session

import java.io.File

internal actual fun clearPlatformCaches(
    httpCacheFolderPath: String,
    appCacheRootPath: String,
) {
    File(httpCacheFolderPath).run {
        listFiles()?.forEach { it.deleteRecursively() }
        mkdirs()
    }

    File(appCacheRootPath).listFiles()?.forEach { entry ->
        if (entry.name.equals("kamel", ignoreCase = true) ||
            entry.name.contains("image", ignoreCase = true)
        ) {
            entry.deleteRecursively()
        }
    }
}
