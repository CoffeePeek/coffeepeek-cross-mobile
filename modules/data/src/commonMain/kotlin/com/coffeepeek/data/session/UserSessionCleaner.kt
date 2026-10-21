package com.coffeepeek.data.session

import com.coffeepeek.domain.repository.FavoriteRepository
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class UserSessionCleaner(
    private val sessionRepository: SessionRepository,
    private val favoriteRepository: FavoriteRepository,
    private val httpCacheFolderPath: String,
    private val appCacheRootPath: String,
    private val clearCaches: () -> Unit = {
        clearPlatformCachesSafely(httpCacheFolderPath, appCacheRootPath)
    },
) {
    suspend fun clearLocalUserData() = withContext(NonCancellable) {
        val owner = sessionRepository.getSession()?.userId
        try {
            sessionRepository.saveSession(null)
            favoriteRepository.clearForUser(owner)
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
