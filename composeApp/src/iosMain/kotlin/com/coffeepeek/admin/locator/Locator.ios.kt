@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.locator

import com.coffeepeek.room.DatabaseCore
import com.coffeepeek.room.MyRoomRepositoryImpl
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual object Locator {
    private val fileManager = NSFileManager.defaultManager

    actual val appCacheRootPath: String by lazy {
        directoryPath(NSCachesDirectory, "CoffeePeek")
    }

    actual val appFilesRootPath: String by lazy {
        directoryPath(NSApplicationSupportDirectory, "CoffeePeek")
    }

    actual val cacheFolderPath: String by lazy {
        ensureDirectory("$appCacheRootPath/http")
    }

    actual val platformContext: Any? = null

    actual val database: DatabaseCore by lazy {
        MyRoomRepositoryImpl("$appFilesRootPath/${Constants.DB_NAME}")
    }

    private fun directoryPath(directory: ULong, child: String): String {
        val root = NSSearchPathForDirectoriesInDomains(
            directory = directory,
            domainMask = NSUserDomainMask,
            expandTilde = true,
        ).firstOrNull() as? String ?: error("Apple application directory is unavailable")
        return ensureDirectory("$root/$child")
    }

    private fun ensureDirectory(path: String): String {
        fileManager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return path
    }
}
