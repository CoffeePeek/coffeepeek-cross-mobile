package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.ui.screen.shop.CheckInDraft
import com.coffeepeek.admin.ui.screen.shop.CheckInDraftStore
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDateFormatter
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import kotlinx.coroutines.CancellationException

/** One app-owned adapter around the existing process store, not another persistent store.
 * Keep this instance across sheet recreation. Legacy logout/clear/replacement invalidates its guard.
 */
internal class AndroidShopCheckInDraftStore(
    private val legacy: CheckInDraftStore,
    private val dates: ShopCheckInDateFormatter = AndroidShopCheckInDateFormatter(),
) : ShopCheckInDraftStore {
    private var guardedDraft: CheckInDraft? = null
    private var deliveryUnconfirmed = false

    /** Draft ownership uses the internal ID; transport always keeps the public slug.
     * Each modal binding retains its own lease so an older screen cannot write into
     * a replacement draft even after another binding has opened it.
     */
    fun forShop(shopId: String, shopSlug: String): ShopCheckInDraftStore {
        require(shopId.isNotBlank() && shopSlug.isNotBlank())
        val boundSlug = shopSlug
        return object : ShopCheckInDraftStore {
            private var lease: CheckInDraft? = null

            override fun open(initial: ShopCheckInCreateInput): Result<ShopCheckInDraftSnapshot> {
                if (initial.shopSlug != shopSlug) return Result.failure(IllegalArgumentException("Mismatched shop slug"))
                return this@AndroidShopCheckInDraftStore.open(initial.copy(shopSlug = shopId)).map {
                    lease = legacy.peek()
                    it.copy(input = it.input.copy(shopSlug = shopSlug))
                }
            }

            override fun save(snapshot: ShopCheckInDraftSnapshot): Result<Unit> {
                if (snapshot.input.shopSlug != shopSlug || lease == null || legacy.peek() !== lease) {
                    return Result.failure(IllegalStateException("Draft binding was replaced"))
                }
                return this@AndroidShopCheckInDraftStore.save(
                    snapshot.copy(input = snapshot.input.copy(shopSlug = shopId)),
                ).onSuccess { lease = legacy.peek() }
            }

            override fun clear(shopSlug: String): Result<Unit> {
                if (shopSlug != boundSlug || lease == null || legacy.peek() !== lease) {
                    return Result.failure(IllegalStateException("Draft binding was replaced"))
                }
                return this@AndroidShopCheckInDraftStore.clear(shopId).onSuccess { lease = null }
            }
        }
    }

    override fun open(initial: ShopCheckInCreateInput): Result<ShopCheckInDraftSnapshot> = draftResult {
        val draft = legacy.open(initial.shopSlug)
        if (guardedDraft !== draft) deliveryUnconfirmed = false
        guardedDraft = draft
        ShopCheckInDraftSnapshot(
            input = initial.copy(
                text = draft.note,
                rating = ShopRating(coffee = draft.coffeeRating, service = draft.serviceRating, place = draft.placeRating),
                visitedAtIso = dates.visitInstant(draft.visitMillis),
                visibility = if (draft.isPublic) ShopCheckInVisibility.Public else ShopCheckInVisibility.Private,
                drinkSlug = draft.drinkSlug,
                customDrinkName = draft.customDrinkName,
                photos = draft.photos.map { ShopCheckInPhoto(it.bytes, it.fileName, it.contentType) },
            ),
            deliveryUnconfirmed = deliveryUnconfirmed,
        )
    }

    override fun save(snapshot: ShopCheckInDraftSnapshot): Result<Unit> = draftResult {
        val input = snapshot.input
        val current = legacy.peek()
        check(current != null && current.shopId == input.shopSlug && guardedDraft === current) {
            "Draft was cleared or replaced; do not save stale screen state"
        }
        val millis = if (dates.visitInstant(current.visitMillis) == input.visitedAtIso) current.visitMillis
            else requireNotNull(dates.pickerMillis(input.visitedAtIso)) { "Invalid visit date" }
        val updated = current.copy(
            note = input.text, coffeeRating = input.rating.coffee,
            serviceRating = input.rating.service, placeRating = input.rating.place,
            visitMillis = millis, isPublic = input.visibility == ShopCheckInVisibility.Public,
            drinkSlug = input.drinkSlug, customDrinkName = input.customDrinkName,
            drinkName = current.drinkName.takeIf { current.drinkSlug == input.drinkSlug },
            photos = input.photos.map { PickedImage(it.bytes, it.fileName, it.contentType) },
        )
        legacy.save(updated)
        guardedDraft = updated
        deliveryUnconfirmed = snapshot.deliveryUnconfirmed
    }

    override fun clear(shopSlug: String): Result<Unit> = draftResult {
        if (guardedDraft?.shopId != shopSlug) return@draftResult
        check(legacy.peek() === guardedDraft) { "Draft was cleared or replaced; do not clear another screen's draft" }
        legacy.clear(shopSlug)
        if (guardedDraft?.shopId == shopSlug) {
            guardedDraft = null
            deliveryUnconfirmed = false
        }
    }
}

private fun <T> draftResult(block: () -> T): Result<T> = try { Result.success(block()) }
catch (cancelled: CancellationException) { throw cancelled }
catch (error: Exception) { Result.failure(error) }
