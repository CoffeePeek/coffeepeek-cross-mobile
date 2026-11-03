package com.coffeepeek.feature.collections.domain.repository

import com.coffeepeek.feature.collections.domain.model.CollectionPlace
import com.coffeepeek.feature.collections.domain.model.CollectionSummary
import com.coffeepeek.feature.collections.domain.model.SharedCollection

interface CollectionsRepository {
    suspend fun favorites(): Result<List<CollectionPlace>>
    suspend fun create(title: String, places: List<CollectionPlace>): Result<SharedCollection>
    suspend fun read(id: String): Result<SharedCollection>
    suspend fun mine(): Result<List<CollectionSummary>>
    suspend fun revoke(id: String): Result<Unit>
    suspend fun photo(url: String): ByteArray?
}
