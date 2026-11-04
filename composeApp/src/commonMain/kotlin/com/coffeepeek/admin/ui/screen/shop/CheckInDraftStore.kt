package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.domain.model.consumedDrinkIssue
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.currentEpochMillis
import com.coffeepeek.admin.utils.datePickerMillisWithLocalTime

data class CheckInDraft(
    val shopId: String,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkName: String? = null,
    val note: String = "",
    val isPublic: Boolean = true,
    /** Actual visit instant, never a date picker's UTC-midnight day token. */
    val visitMillis: Long,
    val placeRating: Int = 4,
    val serviceRating: Int = 4,
    val coffeeRating: Int = 4,
    val photos: List<PickedImage> = emptyList(),
)

internal fun CheckInDraft.withVisitDate(dateMillis: Long, now: Long = currentEpochMillis()): CheckInDraft =
    copy(visitMillis = datePickerMillisWithLocalTime(dateMillis, visitMillis).coerceAtMost(now))

internal fun CheckInDraft.validationError(now: Long = currentEpochMillis()): CheckInDraftValidationError? = when {
    visitMillis <= 0 || visitMillis > now -> CheckInDraftValidationError.Date
    listOf(coffeeRating, serviceRating, placeRating).any { it !in 1..5 } -> CheckInDraftValidationError.Ratings
    note.trim().length > 1000 -> CheckInDraftValidationError.Note
    photos.size > 5 -> CheckInDraftValidationError.Photos
    else -> consumedDrinkIssue(drinkSlug, customDrinkName)?.let { CheckInDraftValidationError.Drink(it) }
}

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

    /** Android migration bridge can inspect ownership without creating/replacing a draft. */
    internal fun peek(): CheckInDraft? = draft

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

    fun clearAll() { draft = null }

    fun clear(shopId: String) {
        if (draft?.shopId == shopId) {
            draft = null
        }
    }
}
