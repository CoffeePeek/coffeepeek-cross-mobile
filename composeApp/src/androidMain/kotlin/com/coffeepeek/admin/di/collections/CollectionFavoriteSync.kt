package com.coffeepeek.admin.di.collections

import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/** A collection edit invalidates the same account's server-backed heart projection. */
internal suspend fun synchronizeCollectionFavorites(changes: Flow<Unit>, favorites: AccountFavorites) {
    changes.collect {
        try {
            favorites.getFavorites(refresh = true).exceptionOrNull()?.let { failure ->
                if (failure is CancellationException) throw failure
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* The next mutation or explicit refresh can retry this read. */ }
    }
}
