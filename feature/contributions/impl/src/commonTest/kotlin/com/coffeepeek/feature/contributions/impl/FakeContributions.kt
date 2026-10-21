package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.api.ContributionLocation
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.api.GeoPoint
import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeAccount(key: String? = "one") : ContributionsAccount {
    val key = MutableStateFlow(key)
    override val accountKey = this.key
    override fun currentAccountKey() = key.value
}

internal fun <T> pageOf(vararg items: T, total: Int = items.size, pages: Int = if (items.isEmpty()) 0 else 1, current: Int = 1): Result<Paged<T>> =
    Result.success(Paged(items.toList(), total, pages, current))

internal class FakeRepository : ContributionsRepository {
    var shops: suspend (ModerationStatus, Int) -> Result<Paged<ShopSubmission>> = { _, _ -> pageOf() }
    var roasters: suspend (ModerationStatus, Int) -> Result<Paged<RoasterSubmission>> = { _, _ -> pageOf() }
    var changes: suspend (ModerationStatus?, Int) -> Result<Paged<ChangeRequest>> = { _, _ -> pageOf() }
    var change: suspend (String) -> Result<ChangeRequest> = { Result.failure(ContributionFailure(FailureKind.NotFound)) }
    var shopSubmit: suspend (NewShop) -> Result<Unit> = { Result.success(Unit) }
    var roasterSubmit: suspend (NewRoaster) -> Result<RoasterSubmitted> = { Result.success(RoasterSubmitted("r1", "Pending", it.address != null, "ok")) }
    var changeSubmit: suspend (ChangeDraft) -> Result<ChangeRequest> = { Result.success(request("new", it.section)) }
    var changeEdit: suspend (String, ChangeDraft) -> Result<ChangeRequest> = { id, draft -> Result.success(request(id, draft.section)) }
    val drafts = mutableListOf<ChangeDraft>()
    val requests = mutableListOf<String>()

    override suspend fun shops(status: ModerationStatus, page: Int, pageSize: Int) = shops(status, page).also { requests += "shops:$status:$page" }
    override suspend fun roasters(status: ModerationStatus, page: Int, pageSize: Int) = roasters(status, page).also { requests += "roasters:$status:$page" }
    override suspend fun changes(status: ModerationStatus?, page: Int, pageSize: Int, shopSlug: String?, section: ChangeSection?) =
        changes(status, page).also { requests += "changes:$status:$page" }
    override suspend fun change(id: String) = change.invoke(id).also { requests += "change:$id" }
    override suspend fun submitShop(shop: NewShop): Result<Unit> = shopSubmit(shop).also { requests += "submitShop:${shop.name}" }
    override suspend fun submitRoaster(roaster: NewRoaster): Result<RoasterSubmitted> = roasterSubmit(roaster).also { requests += "submitRoaster:${roaster.name}" }
    override suspend fun submitChange(draft: ChangeDraft): Result<ChangeRequest> { drafts += draft; return changeSubmit(draft) }
    override suspend fun editChange(id: String, draft: ChangeDraft): Result<ChangeRequest> { drafts += draft; requests += "edit:$id"; return changeEdit(id, draft) }
}

internal fun shop(id: String, status: ModerationStatus = ModerationStatus.Approved, published: String? = null, reason: String? = null) =
    ShopSubmission(id, "Shop $id", "Street $id", status, reason, published)

internal fun request(id: String, section: ChangeSection = ChangeSection.Description, payload: ChangePayload = ChangePayload(description = "text"), status: ModerationStatus = ModerationStatus.Pending) =
    ChangeRequest(id, "cafe", section, payload, status, rejectionReason = null, createdAtUtc = "2026-10-21T07:00:00Z", updatedAtUtc = null)

internal class FakeCatalogs : ContributionCatalogs {
    var catalogs: suspend () -> Result<Catalogs> = { Result.success(Catalogs()) }
    override suspend fun catalogs() = catalogs.invoke()
    var shop: suspend (String) -> Result<ShopSnapshot> = { Result.success(ShopSnapshot(it, "Shop $it")) }
    var drinks: suspend () -> Result<List<DrinkOption>> = { Result.success(emptyList()) }
    override suspend fun shop(slug: String) = shop.invoke(slug)
    override suspend fun drinks() = drinks.invoke()
}

internal class FakeLocation : ContributionLocation {
    var point: GeoPoint? = null
    var address: String? = null
    var calls = 0
    override suspend fun lastKnown() = point.also { calls++ }
    override suspend fun addressOf(point: GeoPoint) = address
}
