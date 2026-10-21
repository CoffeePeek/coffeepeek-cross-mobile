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
    val changed = draft.changedFields(next)
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

internal fun HomePreparationInput.changedFields(next: HomePreparationInput): Set<HomePreparationField> =
    HomePreparationField.entries.filter { field -> when (field) {
        Date -> preparedAt != next.preparedAt || preparedOn != next.preparedOn
        Method -> brewMethodCode != next.brewMethodCode
        Bean -> coffeeProductSlug != next.coffeeProductSlug || coffeeProductId != next.coffeeProductId || customBeanName != next.customBeanName
        Dose -> doseGrams != next.doseGrams
        Duration -> brewDurationSeconds != next.brewDurationSeconds
        Rating -> tasteScore != next.tasteScore
        Note -> text != next.text
        Drink -> drinkSlug != next.drinkSlug || customDrinkName != next.customDrinkName
        Photos -> photos != next.photos
        Visibility -> visibility != next.visibility
    } }.toSet()
