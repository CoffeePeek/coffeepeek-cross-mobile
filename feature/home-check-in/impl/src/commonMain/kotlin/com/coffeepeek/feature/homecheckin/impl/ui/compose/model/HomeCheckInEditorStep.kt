package com.coffeepeek.feature.homecheckin.impl.ui.compose.model

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationOperation

internal enum class HomeCheckInEditorStep { Experience, Details }

internal fun Set<HomePreparationField>.homeEditorStep(): HomeCheckInEditorStep? = when {
    any { it == HomePreparationField.Rating || it == HomePreparationField.Note || it == HomePreparationField.Photos } ->
        HomeCheckInEditorStep.Experience
    isNotEmpty() -> HomeCheckInEditorStep.Details
    else -> null
}

internal fun HomeCheckInEditorStep.previousHomeEditorStep(editing: Boolean): HomeCheckInEditorStep? =
    HomeCheckInEditorStep.Experience.takeIf { !editing && this == HomeCheckInEditorStep.Details }

internal fun HomeCheckInEditorState.homeEditorErrorStep(): HomeCheckInEditorStep? =
    fieldErrors.keys.homeEditorStep() ?: HomeCheckInEditorStep.Experience.takeIf {
        saveIssue != null && failedOperation == HomePreparationOperation.PhotoUpload
    }
