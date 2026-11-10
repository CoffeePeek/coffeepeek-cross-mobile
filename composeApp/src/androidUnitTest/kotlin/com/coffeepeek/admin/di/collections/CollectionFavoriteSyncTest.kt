package com.coffeepeek.admin.di.collections

import com.coffeepeek.feature.favorites.domain.model.FavoriteEntry
import com.coffeepeek.feature.favorites.domain.model.FavoriteKind
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.admin.di.favorites.PublicAddress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionFavoriteSyncTest {
    @Test fun aNewCollectionChangeCancelsAnObsoleteBackgroundRefresh() = runTest {
        val changes = MutableSharedFlow<Unit>()
        val favorites = FavoriteProjection().apply { blockFirstRead = CompletableDeferred() }
        backgroundScope.launch { synchronizeCollectionFavorites(changes, favorites) }
        runCurrent()
        changes.emit(Unit)
        runCurrent()
        changes.emit(Unit)
        runCurrent()
        assertEquals(2, favorites.reads)
        assertTrue(favorites.cancelledFirstRead)
    }

    @Test fun eachCollectionChangeRefreshesServerHeartsEvenAfterAFailedRead() = runTest {
        val changes = MutableSharedFlow<Unit>()
        val favorites = FavoriteProjection()
        backgroundScope.launch { synchronizeCollectionFavorites(changes, favorites) }
        runCurrent()
        changes.emit(Unit)
        runCurrent()
        favorites.fail = false
        changes.emit(Unit)
        runCurrent()
        assertEquals(2, favorites.reads)
        assertTrue(favorites.refresh)
    }

    private class FavoriteProjection : AccountFavorites {
        var reads = 0
        var refresh = false
        var fail = true
        var blockFirstRead: CompletableDeferred<Unit>? = null
        var cancelledFirstRead = false
        override fun observeFavorites() = flowOf(Result.success(emptyList<FavoriteEntry>()))
        override suspend fun getFavorites(refresh: Boolean): Result<List<FavoriteEntry>> {
            reads++
            this.refresh = refresh
            if (reads == 1) try { blockFirstRead?.await() }
            catch (cancelled: CancellationException) { cancelledFirstRead = true; throw cancelled }
            return if (fail) Result.failure(IllegalStateException("Offline")) else Result.success(emptyList())
        }
        override suspend fun setFavorite(entry: FavoriteEntry, favorite: Boolean) = error("Read only")
        override fun clearCache() = Unit
    }
}
