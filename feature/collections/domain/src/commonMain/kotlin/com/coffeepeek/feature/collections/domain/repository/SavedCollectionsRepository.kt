package com.coffeepeek.feature.collections.domain.repository

import com.coffeepeek.feature.collections.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Owner-managed lists; application composition refreshes the server heart projection after changes. */
interface SavedCollectionsRepository {
    /** Successful owner mutations invalidate account-scoped projections. */
    fun observeChanges(): Flow<Unit> = emptyFlow()
    suspend fun mine(target: SavedCollectionTarget? = null): Result<List<SavedCollectionSummary>>
    suspend fun owned(id: String): Result<SavedCollection>
    suspend fun public(id: String): Result<SavedCollection>
    suspend fun create(metadata: SavedCollectionMetadata): Result<SavedCollection>
    suspend fun update(id: String, metadata: SavedCollectionMetadata, revision: Long): Result<SavedCollection>
    suspend fun replaceItems(id: String, places: List<CollectionPlace>, revision: Long): Result<SavedCollection>
    suspend fun memberships(target: SavedCollectionTarget, collectionIds: Set<String>): Result<List<SavedCollectionSummary>>
    suspend fun changeMemberships(target: SavedCollectionTarget, added: Set<String>, removed: Set<String>): Result<List<SavedCollectionSummary>> {
        val result = mine(target)
        val lists = result.getOrNull() ?: return result
        val current = lists.filter { it.containsItem }.map { it.id }.toSet()
        return memberships(target, (current + added) - removed)
    }
    suspend fun delete(id: String): Result<Unit>
    suspend fun featuredId(): Result<String?>
    suspend fun setFeatured(id: String?): Result<Unit>
    suspend fun featured(profileSlug: String): Result<SavedCollection?>
    suspend fun uploadCover(input: CollectionCoverInput): Result<CollectionCoverUpload>
    suspend fun cover(collectionId: String, cover: SavedCollectionCover): Result<ByteArray>
}
