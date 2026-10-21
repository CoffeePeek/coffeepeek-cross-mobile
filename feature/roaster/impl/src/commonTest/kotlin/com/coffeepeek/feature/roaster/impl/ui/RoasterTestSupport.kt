package com.coffeepeek.feature.roaster.impl.ui

import com.coffeepeek.feature.roaster.api.RoasterContentAction
import com.coffeepeek.feature.roaster.api.RoasterDetailAnalytics
import com.coffeepeek.feature.roaster.api.RoasterDetailSection
import com.coffeepeek.feature.roaster.api.RoasterFavoriteTarget
import com.coffeepeek.feature.roaster.api.RoasterFavoritesPort
import com.coffeepeek.feature.roaster.api.RoasterListAnalytics
import com.coffeepeek.feature.roaster.domain.model.RoasterAddress
import com.coffeepeek.feature.roaster.domain.model.RoasterContact
import com.coffeepeek.feature.roaster.domain.model.RoasterDetails
import com.coffeepeek.feature.roaster.domain.model.RoasterPhoto
import com.coffeepeek.feature.roaster.domain.model.RoasterShop
import com.coffeepeek.feature.roaster.domain.model.RoasterSummary
import com.coffeepeek.feature.roaster.domain.model.RoasterTag
import com.coffeepeek.feature.roaster.domain.repository.RoasterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeRoasterRepository : RoasterRepository {
    var catalogCalls = 0
    val detailCalls = mutableListOf<String>()
    var catalog: suspend () -> Result<List<RoasterSummary>> = { Result.success(listOf(summary("a"), summary("b"))) }
    var detail: suspend (String) -> Result<RoasterDetails> = { Result.success(details(it)) }
    override suspend fun getRoasters(): Result<List<RoasterSummary>> { catalogCalls++; return catalog() }
    override suspend fun getRoaster(slug: String): Result<RoasterDetails> { detailCalls += slug; return detail(slug) }
}

internal class FakeFavorites : RoasterFavoritesPort {
    val keys = MutableStateFlow<Set<String>>(emptySet())
    val errors = MutableSharedFlow<String?>(extraBufferCapacity = 8)
    val changes = mutableListOf<Pair<RoasterFavoriteTarget, Boolean>>()
    var failChange = false
    var keysFlow: Flow<Set<String>> = keys.asStateFlow()
    override fun observeKeys(): Flow<Set<String>> = keysFlow
    override fun observeErrors(): Flow<String?> = errors
    override fun change(target: RoasterFavoriteTarget, favorite: Boolean, onFailed: () -> Unit) {
        changes += target to favorite
        if (failChange) onFailed()
    }
}

internal class RecordingListAnalytics : RoasterListAnalytics {
    val calls = mutableListOf<String>()
    override fun resultsShown(fingerprint: String, query: String, filterKeys: List<String>, resultCount: Int, failed: Boolean) {
        calls += "shown:$fingerprint:$filterKeys:$resultCount:$failed"
    }
    override fun opened(position: Int) { calls += "open:$position" }
}

internal class RecordingDetailAnalytics : RoasterDetailAnalytics {
    val calls = mutableListOf<String>()
    override fun contentInteracted(section: RoasterDetailSection, action: RoasterContentAction) { calls += "$section:$action" }
    override fun shopOpened() { calls += "shop" }
}

internal fun address(slug: String) = RoasterAddress(slug, "/roasters/$slug", 1, false)

internal fun summary(
    slug: String?, name: String = "Roaster ${slug.orEmpty()}", tags: List<RoasterTag> = emptyList(), photo: String? = null,
) = RoasterSummary(slug?.let(::address), name, photo, tags, 1, 2, 3)

internal fun tag(slug: String, order: Int = 0) = RoasterTag(slug, slug.uppercase(), null, order)

internal fun details(
    slug: String,
    shops: List<RoasterShop> = emptyList(),
    photos: List<RoasterPhoto> = listOf(RoasterPhoto("p1", "https://m/$slug-1", 1)),
    contact: RoasterContact? = null,
) = RoasterDetails(slug, address(slug), "Roaster $slug", null, null, contact, photos, shops)

internal fun shop(slug: String, photo: String? = null) = RoasterShop(slug, "Shop $slug", photo, address(slug))
