package com.coffeepeek.feature.homecheckin.impl.ui.data

import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField.*
import com.coffeepeek.feature.homecheckin.domain.usecase.homePreparationFieldErrors
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInEditorState
import kotlin.time.Instant

/** Preserve historical identity, nullable visibility and server photos while editing recipe fields. */
internal fun HomePreparation.editorInput() = HomePreparationInput(
    preparedAt = preparedAtUtc, preparedOn = preparedOn, brewMethodCode = brewMethodCode,
    coffeeProductId = coffeeProductId, customBeanName = beanName.takeIf { coffeeProductId == null },
    doseGrams = doseGrams, brewDurationSeconds = brewDurationSeconds, tasteScore = tasteScore, text = text,
    drinkSlug = drinkSlug ?: "other".takeIf { !customDrinkName.isNullOrBlank() || !drinkName.isNullOrBlank() },
    customDrinkName = customDrinkName ?: drinkName.takeIf { drinkSlug == null }, visibility = visibility,
)

/** Only changed fields lose/revalidate their previous errors; keep unrelated server failures. */
internal fun HomeCheckInEditorState.withDraft(next: HomePreparationInput, now: Instant): HomeCheckInEditorState {
    val changed = HomePreparationField.entries.filter { field -> when (field) {
        Date -> draft.preparedAt != next.preparedAt || draft.preparedOn != next.preparedOn
        Method -> draft.brewMethodCode != next.brewMethodCode
        Bean -> draft.coffeeProductSlug != next.coffeeProductSlug || draft.coffeeProductId != next.coffeeProductId || draft.customBeanName != next.customBeanName
        Dose -> draft.doseGrams != next.doseGrams
        Duration -> draft.brewDurationSeconds != next.brewDurationSeconds
        Rating -> draft.tasteScore != next.tasteScore
        Note -> draft.text != next.text
        Drink -> draft.drinkSlug != next.drinkSlug || draft.customDrinkName != next.customDrinkName
        Photos -> draft.photos != next.photos || draft.drinkPhoto != next.drinkPhoto
        Visibility -> draft.visibility != next.visibility
    } }.toSet()
    val local = if (fieldErrors.keys.any { it in changed }) homePreparationFieldErrors(next, entry != null, now) else emptyMap()
    val remaining = fieldErrors.toMutableMap().apply {
        changed.forEach { field -> if (field in this) {
            remove(field)
            local[field]?.let { put(field, it) }
        } }
    }
    return copy(draft = next, fieldErrors = remaining,
        beanSelectionName = beanSelectionName.takeIf { next.coffeeProductSlug != null || next.coffeeProductId != null })
}
