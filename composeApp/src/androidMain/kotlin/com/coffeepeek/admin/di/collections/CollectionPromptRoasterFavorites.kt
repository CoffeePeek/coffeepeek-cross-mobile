package com.coffeepeek.admin.di.collections

import com.coffeepeek.admin.feature.favorites.api.RoasterFavorites
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.model.CatalogItem
import com.coffeepeek.feature.collections.api.CollectionTarget
import com.coffeepeek.feature.collections.api.CollectionTargetKind
import kotlinx.coroutines.CancellationException

/** Android presentation delivery around the existing writer; iOS keeps its original binding. */
internal class CollectionPromptRoasterFavorites(private val delegate: RoasterFavorites,
    private val sessions: SessionReader, private val requests: CollectionPickerRequests) : RoasterFavorites by delegate {
    override suspend fun setFavorite(roaster: CatalogItem, isFavorite: Boolean): Result<Unit> {
        val owner = owner() ?: throw CancellationException("Favorites account changed")
        val lease = requests.lease(owner)
        val result = delegate.setFavorite(roaster, isFavorite)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        if (owner() != owner) throw CancellationException("Favorites account changed")
        if (result.isSuccess && isFavorite) roaster.address?.slug?.let { slug ->
            requests.added(lease, CollectionTarget(CollectionTargetKind.Roaster, slug))
        }
        return result
    }

    private fun owner() = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)
}
