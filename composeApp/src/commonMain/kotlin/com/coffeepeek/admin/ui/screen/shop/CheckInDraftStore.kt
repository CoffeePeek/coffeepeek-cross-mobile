package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.domain.model.validateConsumedDrink
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.currentEpochMillis

data class CheckInDraft(
    val shopId: String,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkName: String? = null,
    val note: String = "",
    val isPublic: Boolean = false,
    val visitMillis: Long,
    val placeRating: Int = 4,
    val serviceRating: Int = 4,
    val coffeeRating: Int = 4,
    val photos: List<PickedImage> = emptyList(),
)

internal fun CheckInDraft.validationError(now: Long = currentEpochMillis()): String? = when {
    visitMillis <= 0 || visitMillis > now -> "Выберите дату визита не позднее сегодняшней"
    listOf(coffeeRating, serviceRating, placeRating).any { it !in 1..5 } -> "Оцените кофе, сервис и атмосферу"
    note.trim().isEmpty() -> "Введите заметку"
    note.trim().length > 1000 -> "Заметка должна быть не длиннее 1000 символов"
    photos.size > 5 -> "Можно добавить до 5 фото"
    else -> validateConsumedDrink(drinkSlug, customDrinkName)
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
