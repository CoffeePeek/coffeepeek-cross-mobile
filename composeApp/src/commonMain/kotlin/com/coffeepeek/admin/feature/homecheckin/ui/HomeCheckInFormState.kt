package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.feature.homecheckin.domain.HomeCheckInField
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInField.*
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInInput
import com.coffeepeek.feature.homecheckin.domain.homeCheckInFieldErrors

internal fun homeValidationSummary(count: Int): String {
    val noun = when {
        count % 100 in 11..14 -> "ошибок"
        count % 10 == 1 -> "ошибка"
        count % 10 in 2..4 -> "ошибки"
        else -> "ошибок"
    }
    return "Проверьте заполненные поля ($count $noun)."
}

/** Keep errors on untouched fields, including failures returned by the server. */
internal fun HomeCheckInUiState.withDraft(next: HomeCheckInInput): HomeCheckInUiState {
    val changed = HomeCheckInField.entries.filter { field ->
        when (field) {
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
        }
    }.toSet()
    val localErrors = if (fieldErrors.keys.any { it in changed }) homeCheckInFieldErrors(next, selected != null) else emptyMap()
    val remaining = fieldErrors.toMutableMap().apply {
        changed.forEach { field ->
            if (field in this) {
                remove(field)
                localErrors[field]?.let { put(field, it) }
            }
        }
    }
    return copy(draft = next, fieldErrors = remaining,
        catalogSelectionName = catalogSelectionName.takeIf { next.coffeeProductSlug != null || next.coffeeProductId != null })
}
