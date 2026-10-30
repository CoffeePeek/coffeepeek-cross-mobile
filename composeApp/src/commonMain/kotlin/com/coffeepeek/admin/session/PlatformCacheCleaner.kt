package com.coffeepeek.admin.session

internal expect fun clearPlatformCaches(
    httpCacheFolderPath: String,
    appCacheRootPath: String,
)
