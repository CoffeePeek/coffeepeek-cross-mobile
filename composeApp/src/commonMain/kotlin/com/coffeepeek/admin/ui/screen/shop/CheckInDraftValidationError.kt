package com.coffeepeek.admin.ui.screen.shop

import com.coffeepeek.admin.ui.screen.review.message

import com.coffeepeek.admin.ui.screen.review.consumedDrinkValidationLabels

import com.coffeepeek.admin.ui.screen.review.ConsumedDrinkValidationLabels

import com.coffeepeek.domain.model.ConsumedDrinkIssue

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

sealed interface CheckInDraftValidationError {
    data object Date : CheckInDraftValidationError
    data object Ratings : CheckInDraftValidationError
    data object Note : CheckInDraftValidationError
    data object Photos : CheckInDraftValidationError
    data class Drink(val issue: ConsumedDrinkIssue) : CheckInDraftValidationError
}

internal data class CheckInDraftValidationLabels(val date: String, val ratings: String, val note: String, val photos: String, val drink: ConsumedDrinkValidationLabels)

@Composable
internal fun checkInDraftValidationLabels() = CheckInDraftValidationLabels(
    stringResource(Res.string.legacy_validation_visit_date),
    stringResource(Res.string.legacy_validation_visit_ratings),
    stringResource(Res.string.legacy_validation_visit_note),
    stringResource(Res.string.legacy_validation_visit_photos),
    consumedDrinkValidationLabels(),
)

internal fun CheckInDraftValidationError.message(labels: CheckInDraftValidationLabels): String = when (this) {
    CheckInDraftValidationError.Date -> labels.date
    CheckInDraftValidationError.Ratings -> labels.ratings
    CheckInDraftValidationError.Note -> labels.note
    CheckInDraftValidationError.Photos -> labels.photos
    is CheckInDraftValidationError.Drink -> issue.message(labels.drink)
}
