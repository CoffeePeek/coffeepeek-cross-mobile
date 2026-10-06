package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.currentEpochMillis

data class CheckInDraft(
    val shopId: String,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkName: String? = null,
    val header: String = "",
    val note: String = "",
    val isPublic: Boolean = false,
    val visitMillis: Long,
    val placeRating: Int = 4,
    val serviceRating: Int = 4,
    val coffeeRating: Int = 4,
    val photos: List<PickedImage> = emptyList(),
)

/**
 * Keeps one check-in draft for the lifetime of the app process.
 *
 * Closing a sheet does not discard the draft. Starting a check-in for another
 * shop replaces it, while restarting the application naturally clears it.
 */
class CheckInDraftStore(
    private val now: () -> Long = ::currentEpochMillis,
) {
    private var draft: CheckInDraft? = null

    fun open(shopId: String): CheckInDraft {
        val current = draft
        if (current?.shopId == shopId) return current

        return CheckInDraft(shopId = shopId, visitMillis = now()).also { draft = it }
    }

    fun save(value: CheckInDraft) {
        if (draft?.shopId == value.shopId) {
            draft = value
        }
    }

    fun clear(shopId: String) {
        if (draft?.shopId == shopId) {
            draft = null
        }
    }
}
