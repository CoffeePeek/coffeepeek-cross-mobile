package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.admin.ui.component.pluralCountLabel
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInField
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInField.*
import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInInput
import com.coffeepeek.legacy.homecheckin.domain.homeCheckInFieldErrors

internal fun homeValidationSummary(count: Int, forms: List<String>, template: String): String =
    template.replace("%1\$s", pluralCountLabel(count, forms))

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
            Photos -> draft.photos != next.photos
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
