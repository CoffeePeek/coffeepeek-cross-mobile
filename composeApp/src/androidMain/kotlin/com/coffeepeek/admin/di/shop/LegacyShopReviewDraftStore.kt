package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.settings.ReviewDraft
import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.ShopReviewPhoto
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopReviewDraftStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Reuses the existing Android draft key and 30-day/debounced storage policy. */
fun createLegacyShopReviewCreateDraftStore(
    shopId: String,
    legacy: ReviewDraftStore,
): ShopReviewDraftStore = LegacyShopReviewDraftStore(
    legacy = legacy,
    key = ReviewDraftStore.newReviewKey(shopId),
    defaultRating = 4,
)

/** Edits use the published review as baseline, so the legacy blank-draft sentinel is disabled. */
fun createLegacyShopReviewEditDraftStore(
    reviewId: String,
    legacy: ReviewDraftStore,
): ShopReviewDraftStore = LegacyShopReviewDraftStore(
    legacy = legacy,
    key = ReviewDraftStore.editReviewKey(reviewId),
    defaultRating = -1,
)

private class LegacyShopReviewDraftStore(
    private val legacy: ReviewDraftStore,
    private val key: String,
    private val defaultRating: Int,
) : ShopReviewDraftStore {
    override suspend fun load(): Result<ShopReviewDraftSnapshot?> = try {
        val saved = legacy.load(key)
        // Legacy load uses runCatching internally; do not turn cancellation into an empty draft.
        currentCoroutineContext().ensureActive()
        val photos = legacy.photos(key).map(PickedImage::toFeature)
        Result.success(if (saved == null && photos.isEmpty()) null else ShopReviewDraftSnapshot(
            header = saved?.header.orEmpty(),
            comment = saved?.comment.orEmpty(),
            rating = ShopRating(
                saved?.placeRating ?: defaultRating,
                saved?.serviceRating ?: defaultRating,
                saved?.coffeeRating ?: defaultRating,
            ),
            photos = photos,
        ))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    override fun save(draft: ShopReviewDraftSnapshot): Result<Unit> = try {
        legacy.save(key,
            ReviewDraft(draft.header, draft.comment, draft.rating.place,
                draft.rating.service, draft.rating.coffee),
            draft.photos.map(ShopReviewPhoto::toLegacy),
            defaultRating = defaultRating)
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun clear(): Result<Unit> = try {
        legacy.clear(key)
        currentCoroutineContext().ensureActive()
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}

private fun PickedImage.toFeature() = ShopReviewPhoto(bytes, fileName, contentType)
private fun ShopReviewPhoto.toLegacy() = PickedImage(bytes, fileName, contentType)
