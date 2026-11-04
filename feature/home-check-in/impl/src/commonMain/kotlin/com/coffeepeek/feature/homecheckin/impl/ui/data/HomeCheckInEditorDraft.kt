package com.coffeepeek.feature.homecheckin.impl.ui.data

import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField.*
import com.coffeepeek.feature.homecheckin.domain.usecase.homePreparationFieldErrors
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInEditorState
import kotlin.time.Instant

/** Preserve historical identity, nullable visibility and server photos while editing recipe fields. */
internal fun HomePreparation.editorInput() = HomePreparationInput(
    preparedAt = preparedAtUtc, preparedOn = preparedOn, brewMethodCode = brewMethodCode,
    // A linked bag keeps its snapshots on the server; sending the bag alone is how an edit keeps the link.
    shelfBagId = shelfBagId, coffeeProductId = coffeeProductId.takeIf { shelfBagId == null },
    customBeanName = beanName.takeIf { coffeeProductId == null && shelfBagId == null },
    doseGrams = doseGrams, brewDurationSeconds = brewDurationSeconds, tasteScore = tasteScore, text = text,
    drinkSlug = drinkSlug ?: "other".takeIf { !customDrinkName.isNullOrBlank() || !drinkName.isNullOrBlank() },
    customDrinkName = customDrinkName ?: drinkName.takeIf { drinkSlug == null }, visibility = visibility,
    // The entry keeps its copy of the card only while it is asked to show the same bag again.
    showShelfBag = shelfBagPhoto != null,
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
        beanSelectionName = beanSelectionName.takeIf {
            next.coffeeProductSlug != null || next.coffeeProductId != null || next.shelfBagId != null
        },
        shelfBagCardUrl = shelfBagCardUrl.takeIf { next.shelfBagId != null && next.shelfBagId == draft.shelfBagId })
}

internal fun HomePreparationInput.changedFields(next: HomePreparationInput): Set<HomePreparationField> =
    HomePreparationField.entries.filter { field -> when (field) {
        Date -> preparedAt != next.preparedAt || preparedOn != next.preparedOn
        Method -> brewMethodCode != next.brewMethodCode
        Bean -> coffeeProductSlug != next.coffeeProductSlug || coffeeProductId != next.coffeeProductId ||
            customBeanName != next.customBeanName || shelfBagId != next.shelfBagId
        Dose -> doseGrams != next.doseGrams
        Duration -> brewDurationSeconds != next.brewDurationSeconds
        Rating -> tasteScore != next.tasteScore
        Note -> text != next.text
        Drink -> drinkSlug != next.drinkSlug || customDrinkName != next.customDrinkName
        Photos -> photos != next.photos
        Visibility -> visibility != next.visibility
    } }.toSet()

/**
 * A coffee a new preparation starts with: the owner's shelf bag [shelfBagId] (shown as [name]), else a catalogue coffee by
 * [slug] (shown as [name]), or else one of the user's own named [name].
 */
internal data class HomeCheckInStartBean(val slug: String?, val name: String?, val shelfBagId: String? = null)

/** A new form; with [bean] its coffee is already chosen, exactly as the bean picker would have set it. */
internal fun homeCheckInStartState(preparedAt: String, preparedOn: String, bean: HomeCheckInStartBean?): HomeCheckInEditorState {
    val bagId = bean?.shelfBagId?.trim()?.ifBlank { null }
    val slug = bean?.slug?.trim()?.ifBlank { null }?.takeIf { bagId == null }
    val name = bean?.name?.trim()?.take(250)?.ifBlank { null }
    val draft = HomePreparationInput(preparedAt = preparedAt, preparedOn = preparedOn, tasteScore = 5, coffeeProductSlug = slug,
        customBeanName = name.takeIf { slug == null && bagId == null }, shelfBagId = bagId)
    return HomeCheckInEditorState(draft = draft, beanSelectionName = name.takeIf { slug != null || bagId != null })
}
