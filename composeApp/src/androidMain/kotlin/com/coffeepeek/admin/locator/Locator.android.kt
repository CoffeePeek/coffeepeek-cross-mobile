package com.coffeepeek.admin.locator

import com.coffeepeek.admin.CoffeePeekApplication
import com.coffeepeek.room.DatabaseCore
import com.coffeepeek.room.MyRoomRepositoryImpl
import java.io.File

actual object Locator {
    val appContext get() = CoffeePeekApplication.context

    actual val cacheFolderPath: String by lazy {
        File(appContext.cacheDir, "cache").apply { mkdirs() }.absolutePath
    }

    actual val appCacheRootPath: String
        get() = appContext.cacheDir.absolutePath

    actual val appFilesRootPath: String
        get() = appContext.noBackupFilesDir.absolutePath

    actual val platformContext: Any?
        get() = appContext

    actual val database: DatabaseCore by lazy {
        MyRoomRepositoryImpl(appContext, Constants.DB_NAME)
    }
}
