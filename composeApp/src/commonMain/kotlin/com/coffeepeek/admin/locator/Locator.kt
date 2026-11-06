package com.coffeepeek.admin.locator

import com.coffeepeek.room.DatabaseCore

expect object Locator {
    val cacheFolderPath: String
    val appCacheRootPath: String
    val appFilesRootPath: String
    val platformContext: Any?
    val database: DatabaseCore
}
