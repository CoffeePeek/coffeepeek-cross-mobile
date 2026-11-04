package com.coffeepeek.feature.collections.impl.ui

import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow

internal val savedShop = CollectionPlace(CollectionPlaceKind.CoffeeShop, "shop", "Shop")
internal val privateList = SavedCollection(SavedCollectionSummary("private", "Private", itemCount = 1), listOf(savedShop))
internal val publicList = SavedCollection(SavedCollectionSummary("public", "Public", visibility = SavedCollectionVisibility.Public,
    itemCount = 1, url = "https://fixture.invalid/collections/public"), listOf(savedShop))

internal class SavedTestAccount : CollectionsAccount {
    val owner = MutableStateFlow<String?>("first")
    var loads = 0
    override val identities = owner
    override fun currentId() = owner.value
    override suspend fun loadId(): String? { loads++; return owner.value }
}

internal class SavedTestSnapshots : CollectionsRepository {
    var favoriteReads = 0
    var creates = 0
    override suspend fun favorites(): Result<List<CollectionPlace>> {
        favoriteReads++
        return Result.success(listOf(savedShop, savedShop.copy(slug = "second")))
    }
    override suspend fun create(title: String, places: List<CollectionPlace>): Result<SharedCollection> {
        creates++
        return Result.success(SharedCollection("snapshot", title, "https://fixture.invalid", places))
    }
    override suspend fun read(id: String) = Result.failure<SharedCollection>(IllegalStateException("Unused"))
    override suspend fun mine() = Result.success(emptyList<CollectionSummary>())
    override suspend fun revoke(id: String) = Result.success(Unit)
    override suspend fun photo(url: String): ByteArray? = null
}

internal class SavedTestRepository : SavedCollectionsRepository {
    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override fun observeChanges() = changes
    var value = privateList
    var lists = listOf(privateList.summary, publicList.summary)
    var featuredValue: SavedCollection? = publicList
    var featuredIdValue: String? = publicList.summary.id
    var creates = 0
    var updates = 0
    var itemWrites = 0
    var uploads = 0
    var membershipWrites = 0
    var featureWrites = 0
    var publicReads = 0
    var sentMemberships = emptySet<String>()
    var sentTarget: SavedCollectionTarget? = null
    var uploadFailure = false
    var uploadError: Throwable? = null
    var updateFailure: Throwable? = null
    var mineFailure: Throwable? = null
    var mineGate: CompletableDeferred<Unit>? = null
    var mineReads = 0
    var membershipGate: CompletableDeferred<Unit>? = null
    var featuredGate: CompletableDeferred<Unit>? = null

    override suspend fun mine(target: SavedCollectionTarget?): Result<List<SavedCollectionSummary>> {
        mineReads++
        val result = mineFailure?.let { Result.failure(it) } ?: Result.success(lists)
        mineGate?.await()
        return result
    }
    override suspend fun owned(id: String) = Result.success(value)
    override suspend fun public(id: String): Result<SavedCollection> { publicReads++; return Result.success(publicList) }
    override suspend fun create(metadata: SavedCollectionMetadata): Result<SavedCollection> {
        creates++
        value = SavedCollection(SavedCollectionSummary("created", metadata.title.trim(), metadata.description.trim(), metadata.visibility))
        return Result.success(value)
    }
    override suspend fun update(id: String, metadata: SavedCollectionMetadata, revision: Long): Result<SavedCollection> {
        updates++
        updateFailure?.let { return Result.failure(it) }
        value = value.copy(summary = value.summary.copy(title = metadata.title.trim(), description = metadata.description.trim(),
            visibility = metadata.visibility, revision = revision + 1))
        return Result.success(value)
    }
    override suspend fun replaceItems(id: String, places: List<CollectionPlace>, revision: Long): Result<SavedCollection> {
        itemWrites++
        value = value.copy(summary = value.summary.copy(itemCount = places.size, revision = revision + 1), places = places)
        return Result.success(value)
    }
    override suspend fun memberships(target: SavedCollectionTarget, collectionIds: Set<String>): Result<List<SavedCollectionSummary>> {
        membershipWrites++
        sentTarget = target
        sentMemberships = collectionIds
        membershipGate?.await()
        return Result.success(lists.map { it.copy(containsItem = it.id in collectionIds) })
    }
    override suspend fun delete(id: String) = Result.success(Unit)
    override suspend fun featuredId() = Result.success(featuredIdValue)
    override suspend fun setFeatured(id: String?): Result<Unit> {
        featureWrites++
        featuredIdValue = id
        return Result.success(Unit)
    }
    override suspend fun featured(profileSlug: String): Result<SavedCollection?> {
        featuredGate?.await()
        return Result.success(featuredValue)
    }
    override suspend fun uploadCover(input: CollectionCoverInput): Result<CollectionCoverUpload> {
        uploads++
        uploadError?.let { return Result.failure(it) }
        return if (uploadFailure) Result.failure(IllegalStateException("Upload failed"))
            else Result.success(CollectionCoverUpload(input.fileName, input.mimeType, "collection-covers/owned/file", input.bytes.size.toLong()))
    }
    override suspend fun cover(collectionId: String, cover: SavedCollectionCover) = Result.success(byteArrayOf(1))
}
