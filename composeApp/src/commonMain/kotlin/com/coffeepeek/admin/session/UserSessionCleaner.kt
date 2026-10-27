package com.coffeepeek.admin.session

import com.coffeepeek.core.session.SessionStore
import com.coffeepeek.data.session.LocalUserDataCleanup
import com.coffeepeek.domain.feature.favorites.api.FavoritesRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.koin.dsl.module

/**
 * Ends an account's local life: the stored session first, then the in-memory favorites, then the disk caches.
 * The caches are cleared even when an earlier step fails, and the first failure is reported.
 */
class UserSessionCleaner(
    private val sessions: SessionStore,
    private val clearFavoritesCache: suspend () -> Unit,
    private val clearCaches: () -> Unit,
) : LocalUserDataCleanup {
    override suspend fun clear() = withContext(NonCancellable) {
        try {
            sessions.saveSession(null)
            clearFavoritesCache()
        } finally {
            clearCaches()
        }
    }
}

fun platformCacheClearing(httpCacheFolderPath: String, appCacheRootPath: String): () -> Unit = {
    runCatching { clearPlatformCaches(httpCacheFolderPath, appCacheRootPath) }
}

/** The account teardown shared by the sign-out, a takeover by another account and a server-forced logout. */
internal fun sessionCleanupModule(httpCacheFolderPath: String, appCacheRootPath: String) = module {
    single {
        UserSessionCleaner(
            sessions = get<SessionStore>(),
            clearFavoritesCache = { get<FavoritesRepository>().clearCache() },
            clearCaches = platformCacheClearing(httpCacheFolderPath, appCacheRootPath),
        )
    }
    single<LocalUserDataCleanup> { get<UserSessionCleaner>() }
}
