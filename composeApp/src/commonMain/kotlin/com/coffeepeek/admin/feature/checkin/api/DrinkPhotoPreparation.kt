package com.coffeepeek.admin.feature.checkin.api

import com.coffeepeek.domain.model.PendingPhotoUpload
import kotlinx.coroutines.CoroutineScope

/** Preparation state shared by the home and coffee-shop check-in entry points. */
data class DrinkPhotoState(
    val selected: PendingPhotoUpload? = null,
    val candidates: List<PendingPhotoUpload> = emptyList(),
    val processing: Boolean = false,
    val skipped: Boolean = false,
    val error: String? = null,
    internal val source: PendingPhotoUpload? = null,
    internal val revision: Long = 0,
) {
    val canSubmit: Boolean get() = !processing && (candidates.size <= 1 || selected != null || skipped)
}

/** One first-photo preparation session, owned by the consuming form's lifetime. */
interface DrinkPhotoPreparation {
    val state: DrinkPhotoState
    fun setSource(photo: PendingPhotoUpload?, selected: PendingPhotoUpload? = null, skipped: Boolean = false)
    fun reset()
    fun retry()
    fun skip()
    fun select(photo: PendingPhotoUpload)

    fun interface Factory {
        fun create(scope: CoroutineScope, onChanged: (DrinkPhotoState) -> Unit): DrinkPhotoPreparation

        companion object {
            val Unavailable: Factory
                get() = com.coffeepeek.admin.feature.checkin.ui.unavailableDrinkPhotoPreparationFactory
        }
    }
}