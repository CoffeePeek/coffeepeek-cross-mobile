package com.coffeepeek.feature.homecheckin.domain.usecase

import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationField.*
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue.*
import kotlin.test.*
import kotlin.time.Instant

class HomePreparationValidationTest {
    private val now = Instant.parse("2026-10-17T12:00:00Z")
    private val draft = HomePreparationInput("2026-10-17T10:00:00Z", "2026-10-17")
    private fun errors(input: HomePreparationInput, editing: Boolean = false) =
        homePreparationFieldErrors(input, editing, now)

    @Test fun minimalDraftKeepsOptionalFieldsOptionalAndStartsPrivate() {
        assertTrue(errors(draft).isEmpty())
        assertEquals(HomePreparationVisibility.Private, draft.visibility)
        assertNull(draft.tasteScore) // Default score 5 belongs to new-form presentation, not domain.
    }

    @Test fun collectsEveryInvalidFieldWithoutTransportOrUiText() {
        val errors = errors(draft.copy(preparedAt = "bad", brewMethodCode = "Not a code",
            coffeeProductSlug = "bean", customBeanName = "Other", doseGrams = "-1",
            brewDurationSeconds = 0, tasteScore = 6, text = "a".repeat(1001),
            customDrinkName = "Without Other", photos = List(6) { photo(1) }))
        assertEquals(setOf(Date, Method, Bean, Dose, Duration, Rating, Note, Drink, Photos), errors.keys)
        assertEquals(MissingDateOffset, errors[Date])
    }

    @Test fun doseAndDurationPreserveInclusiveBoundsAndRejectInvalidPrecision() {
        for (value in listOf("0.01", "18.50", "10000")) assertNull(errors(draft.copy(doseGrams = value))[Dose])
        for (value in listOf("NaN", "Infinity", "1e2", "0", "-1", "10000.01", "18.001", "0.001"))
            assertNotNull(errors(draft.copy(doseGrams = value))[Dose], value)
        for (value in listOf(1, 604800)) assertNull(errors(draft.copy(brewDurationSeconds = value))[Duration])
        for (value in listOf(0, 604801)) assertNotNull(errors(draft.copy(brewDurationSeconds = value))[Duration])
    }

    @Test fun historicalBeanIdIsValidOnlyForAnUnambiguousEdit() {
        val input = draft.copy(coffeeProductId = "historical")
        assertEquals(ConflictingBeanSelection, errors(input)[Bean])
        assertNull(errors(input, editing = true)[Bean])
        assertNotNull(errors(input.copy(coffeeProductSlug = "replacement"), editing = true)[Bean])
        assertNotNull(errors(input.copy(customBeanName = "Custom"), editing = true)[Bean])
    }

    @Test fun suppliedTextAndCustomDrinkRespectBounds() {
        assertTrue(errors(draft.copy(text = "a".repeat(1000), customBeanName = "b".repeat(250),
            drinkSlug = "other", customDrinkName = "c".repeat(100))).isEmpty())
        assertEquals(NoteTooLong, errors(draft.copy(text = "a".repeat(1001)))[Note])
        assertEquals(BeanNameTooLong, errors(draft.copy(customBeanName = "b".repeat(251)))[Bean])
        assertEquals(InvalidDrinkName, errors(draft.copy(drinkSlug = "other", customDrinkName = " "))[Drink])
        assertEquals(InvalidDrink, errors(draft.copy(drinkSlug = "espresso", customDrinkName = "Custom"))[Drink])
    }

    @Test fun photosRemainCreateOnlyAndDistinct() {
        assertEquals(PhotoEditUnsupported, errors(draft.copy(photos = listOf(photo(1))), editing = true)[Photos])
        assertEquals(DuplicatePhotos, errors(draft.copy(photos = listOf(photo(1), photo(1))))[Photos])
    }

    @Test fun timestampsRequireOffsetServerPrecisionAndRealCivilDates() {
        for (time in listOf("2026-10-17T10:00:00Z", "2026-10-17T13:00:00+03:00",
            "2026-10-17T10:00:00.1234567Z")) assertNull(errors(draft.copy(preparedAt = time))[Date])
        for (time in listOf("2026-10-17T10:00:00", "2026-10-17T10:00:00.123456789Z",
            "2026-02-30T10:00:00Z", "2026-10-17T25:00:00Z", "2026-10-17T10:00:00+14:01"))
            assertNotNull(errors(draft.copy(preparedAt = time))[Date], time)
        assertEquals(InvalidDate, errors(draft.copy(preparedOn = "2026-02-30"))[Date])
    }

    @Test fun invalidOriginalMetadataIsRejectedBeforeUpload() {
        for (upload in listOf(photo(1).copy(fileName = " "), photo(1).copy(bytes = byteArrayOf()),
            photo(1).copy(contentType = "text/plain"))) {
            assertEquals(InvalidPhotos, errors(draft.copy(photos = listOf(upload)))[Photos])
        }
    }

    @Test fun civilDateIsCheckedAroundUtcDateNotJustIsoTextDate() {
        val nearMidnight = draft.copy(preparedAt = "2026-10-17T00:30:00+14:00")
        assertNull(errors(nearMidnight.copy(preparedOn = "2026-10-15"))[Date])
        assertEquals(CivilDateMismatch, errors(nearMidnight.copy(preparedOn = "2026-10-18"))[Date])
        assertNull(errors(draft.copy(preparedOn = null))[Date])
    }

    @Test fun futureToleranceIsDeterministicAndInclusive() {
        assertNull(errors(draft.copy(preparedAt = "2026-10-17T12:05:00Z"))[Date])
        assertEquals(FutureDate, errors(draft.copy(preparedAt = "2026-10-17T12:05:00.001Z"))[Date])
    }

    @Test fun unknownEditVisibilityIsNotForcedToPrivate() {
        assertTrue(errors(draft.copy(visibility = null), editing = true).isEmpty())
        assertNull(draft.copy(visibility = null).visibility)
    }

    private fun photo(byte: Int, type: String = "image/jpeg") =
        HomePreparationUpload("photo", type, byteArrayOf(byte.toByte()))
}
