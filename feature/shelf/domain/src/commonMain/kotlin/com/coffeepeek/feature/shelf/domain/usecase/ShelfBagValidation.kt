package com.coffeepeek.feature.shelf.domain.usecase

import com.coffeepeek.feature.shelf.domain.model.ShelfBagDetails
import com.coffeepeek.feature.shelf.domain.model.ShelfBean

const val SHELF_TEXT_LIMIT = 250
const val SHELF_LIMIT = 200
val ShelfWeightRange = 1..10000
val ShelfRatingRange = 1..5

/** The server's field limits; the roast date's "not in the future" rule is left to the server's clock. */
fun ShelfBagDetails.isValid(): Boolean {
    val beanValid = when (val bean = bean) {
        ShelfBean.Unknown -> true
        is ShelfBean.Catalog -> bean.slug.isNotBlank()
        is ShelfBean.Kept -> bean.coffeeProductId.isNotBlank()
        is ShelfBean.Custom -> bean.name.trim().length in 1..SHELF_TEXT_LIMIT
    }
    val roasterValid = (roasterName?.trim()?.length ?: 0) <= SHELF_TEXT_LIMIT
    val weightValid = weightGrams == null || weightGrams in ShelfWeightRange
    val dateValid = roastedOn == null || roastedOn.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
    return beanValid && roasterValid && weightValid && dateValid
}

/** A new bag needs a card or a coffee; "Заполню позже" still has the card. */
fun ShelfBagDetails.canCreate(hasCard: Boolean): Boolean = isValid() && (hasCard || bean != ShelfBean.Unknown)
