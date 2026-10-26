package com.coffeepeek.feature.checkinhistory.impl.ui.data

import com.coffeepeek.feature.checkinhistory.domain.model.HistoryRating
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryRecipe

/** A timeline must not invent the missing category scores; the detail screen may show partial scores separately. */
internal fun HistoryRating?.completeTimelineRating(): HistoryRating? =
    this?.takeIf { it.place != null && it.service != null && it.coffee != null }

internal fun HomeHistoryRecipe.timelineDose(): Double? = doseGrams?.toDoubleOrNull()?.takeIf(Double::isFinite)
