package com.coffeepeek.data.session

import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.SessionRepository

class UserSessionCleaner(
    private val sessionRepository: SessionRepository,
    private val favoriteRepository: FavoriteRepository,
    private val httpCacheFolderPath: String,
    private val appCacheRootPath: String,
    private val clearCaches: () -> Unit = {
        clearPlatformCachesSafely(httpCacheFolderPath, appCacheRootPath)
    },
) {
    suspend fun clearLocalUserData() {
        try {
            sessionRepository.saveSession(null)
            favoriteRepository.clearAll()
        } finally {
            clearCaches()
        }
    }

    fun clearDiskCaches() {
        clearPlatformCachesSafely(httpCacheFolderPath, appCacheRootPath)
    }
}

private fun clearPlatformCachesSafely(httpCacheFolderPath: String, appCacheRootPath: String) {
    runCatching { clearPlatformCaches(httpCacheFolderPath, appCacheRootPath) }
}
