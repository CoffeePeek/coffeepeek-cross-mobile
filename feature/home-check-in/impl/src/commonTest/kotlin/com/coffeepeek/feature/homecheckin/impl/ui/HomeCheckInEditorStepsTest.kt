package com.coffeepeek.feature.homecheckin.impl.ui

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationOperation
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeCheckInEditorStepsTest {
    @Test fun experienceErrorsWinOverDetailsErrorsRegardlessOfOrder() {
        for (field in listOf(HomePreparationField.Rating, HomePreparationField.Note, HomePreparationField.Photos)) {
            val errors = setOf(HomePreparationField.Date, HomePreparationField.Visibility, field)
            assertEquals(HomeCheckInEditorStep.Experience, errors.homeEditorStep())
        }
    }

    @Test fun dateDrinkAndEveryRecipeOrVisibilityErrorReturnToDetails() {
        val fields = HomePreparationField.entries - setOf(
            HomePreparationField.Rating, HomePreparationField.Note, HomePreparationField.Photos,
        )
        for (field in fields) assertEquals(HomeCheckInEditorStep.Details, setOf(field).homeEditorStep())
        assertNull(emptySet<HomePreparationField>().homeEditorStep())
    }

    @Test fun backMovesOnlyCreateDetailsToExperience() {
        assertEquals(HomeCheckInEditorStep.Experience, HomeCheckInEditorStep.Details.previousHomeEditorStep(false))
        assertNull(HomeCheckInEditorStep.Experience.previousHomeEditorStep(false))
        for (step in HomeCheckInEditorStep.entries) assertNull(step.previousHomeEditorStep(true))
    }

    @Test fun failedUploadReturnsToPhotosWithoutFieldErrors() {
        val state = HomeCheckInEditorState(saveIssue = HomePreparationIssue.Offline,
            failedOperation = HomePreparationOperation.PhotoUpload)
        assertEquals(HomeCheckInEditorStep.Experience, state.homeEditorErrorStep())
        assertNull(state.copy(failedOperation = HomePreparationOperation.Save).homeEditorErrorStep())
    }

    @Test fun repeatedValidationStillHasDestinationAfterNavigatingForward() {
        val state = HomeCheckInEditorState(fieldErrors = mapOf(HomePreparationField.Note to HomePreparationIssue.NoteTooLong))
        assertEquals(HomeCheckInEditorStep.Experience, state.homeEditorErrorStep())
        assertEquals(HomeCheckInEditorStep.Experience, state.copy(saveAttempt = 2).homeEditorErrorStep())
        assertNull(state.copy(fieldErrors = emptyMap()).homeEditorErrorStep())
    }
}
