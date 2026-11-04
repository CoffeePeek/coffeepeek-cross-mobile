package com.coffeepeek.feature.shelf.impl.ui

import com.coffeepeek.feature.shelf.domain.model.PublicShelf
import com.coffeepeek.feature.shelf.domain.model.PublicShelfItem
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeShelfProfileRepository(owner: String? = "owner") : ShelfProfileRepository {
    override val owners = MutableStateFlow(owner)
    var visibilityRead: suspend () -> Result<Boolean> = { Result.success(false) }
    var visibilityWrite: suspend (Boolean) -> Result<Boolean> = { Result.success(it) }
    var publicRead: suspend (String) -> Result<PublicShelf?> = { Result.success(null) }
    var visibilityReads = 0
    val writes = mutableListOf<Boolean>()
    val publicSlugs = mutableListOf<String>()
    var photoCalls = 0

    override suspend fun visibility(): Result<Boolean> {
        visibilityReads++
        return visibilityRead()
    }

    override suspend fun setVisibility(visible: Boolean): Result<Boolean> {
        writes += visible
        return visibilityWrite(visible)
    }

    override suspend fun publicShelf(slug: String): Result<PublicShelf?> {
        publicSlugs += slug
        return publicRead(slug)
    }

    override suspend fun photo(url: String): Result<ByteArray> {
        photoCalls++
        return Result.failure(IllegalStateException("ViewModel must not load photos"))
    }
}

internal fun profileShelf(slug: String? = "ethiopia") = PublicShelf(
    listOf(PublicShelfItem("bag", "Эфиопия", "Roaster", slug, null)), 1)
