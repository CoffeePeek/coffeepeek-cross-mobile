package com.coffeepeek.feature.shelf.domain.repository

import com.coffeepeek.feature.shelf.domain.model.PublicShelf
import kotlinx.coroutines.flow.Flow

/** Explicit owner opt-in controls a separate, read-only public projection. */
interface ShelfProfileRepository {
    val owners: Flow<String?>
    suspend fun visibility(): Result<Boolean>
    suspend fun setVisibility(visible: Boolean): Result<Boolean>
    suspend fun publicShelf(slug: String): Result<PublicShelf?>
    suspend fun photo(url: String): Result<ByteArray>
}
