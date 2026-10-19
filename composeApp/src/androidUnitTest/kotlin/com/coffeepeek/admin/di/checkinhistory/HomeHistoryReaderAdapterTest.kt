package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.di.homecheckin.HomeReadTestRepository
import com.coffeepeek.admin.di.homecheckin.LegacyHomePreparationReader
import com.coffeepeek.admin.di.homecheckin.homeReadFixture
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInModeration
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPage
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInVisibility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeHistoryReaderAdapterTest {
    @Test fun photoRepresentationsSurviveBothIndependentFeatureBoundaries() = runTest {
        val source = HomeReadTestRepository().apply {
            history = { query -> Result.success(HomeCheckInPage(listOf(homeReadFixture().copy(
                photos = listOf(com.coffeepeek.feature.homecheckin.domain.HomeCheckInPhoto(
                    "photo", "original", "feed", "detail", "thumb")),
                drinkPhotoUrl = "cutout-original", drinkPhotoThumbnailUrl = "cutout-thumb",
                drinkPhotoDetailUrl = "cutout-detail")), 1, query.page, query.size)) }
        }
        val entry = HomeHistoryReaderAdapter(LegacyHomePreparationReader(source)).page(1, 20, null).getOrThrow().items.single()
        assertEquals("original", entry.photos.single().url)
        assertEquals("feed", entry.photos.single().thumbnailUrl)
        assertEquals("detail", entry.photos.single().detailUrl)
        assertEquals("thumb", entry.photos.single().calendarUrl)
        assertEquals("cutout-original", entry.drinkPhotoUrl)
        assertEquals("cutout-thumb", entry.drinkPhotoThumbnailUrl)
        assertEquals("cutout-detail", entry.drinkPhotoDetailUrl)
    }

    @Test fun snapshotsPreserveCivilDateRecipePhotoIdentityAndUnknownPublication() = runTest {
        val source = homeReadFixture()
        val reader = HomeHistoryReaderAdapter(LegacyHomePreparationReader(HomeReadTestRepository()))
        val page = reader.page(1, 20, null).getOrThrow()
        val item = page.items.single()
        assertEquals(source.id, item.id)
        assertEquals("2026-10-16", item.preparedOn) // UTC timestamp is the previous day.
        assertEquals(source.preparedAtUtc, item.preparedAtUtc)
        assertEquals(source.createdAtUtc, item.createdAtUtc)
        assertEquals(source.text, item.note)
        assertEquals(source.brewMethodCode, item.recipe.brewMethodCode)
        assertEquals(source.coffeeProductId, item.recipe.coffeeProductId)
        assertEquals(source.beanName, item.recipe.beanName)
        assertEquals("75.50", item.recipe.doseGrams)
        assertEquals(210, item.recipe.brewDurationSeconds)
        assertEquals(4, item.recipe.tasteScore)
        assertEquals(source.drinkName, item.drinkName)
        assertEquals(source.drinkSlug, item.drinkSlug)
        assertEquals(source.customDrinkName, item.customDrinkName)
        assertEquals(source.photos.single().id, item.photos.single().id)
        assertEquals(source.photos.single().url, item.photos.single().url)
        assertEquals(source.drinkPhotoUrl, item.drinkPhotoUrl)
        assertNull(item.visibility)
        assertEquals("Unknown", item.moderation.name)
        assertTrue(page.hasMore)
    }

    @Test fun allVisibilityAndModerationStatesSurviveBothBoundaries() = runTest {
        val source = HomeReadTestRepository()
        val reader = HomeHistoryReaderAdapter(LegacyHomePreparationReader(source))
        for (visibility in listOf(null) + HomeCheckInVisibility.entries) {
            for (moderation in HomeCheckInModeration.entries) {
                source.history = { query -> Result.success(HomeCheckInPage(
                    listOf(homeReadFixture().copy(visibility = visibility, moderation = moderation)), 1, query.page, query.size)) }
                val item = reader.page(1, 20, null).getOrThrow().items.single()
                assertEquals(visibility?.name, item.visibility?.name)
                assertEquals(moderation.name, item.moderation.name)
            }
        }
    }

    @Test fun pagingAndCalendarPreserveExistingAllDatesSingleDayAndLastDayQueries() = runTest {
        val source = HomeReadTestRepository()
        val reader = HomeHistoryReaderAdapter(LegacyHomePreparationReader(source))
        val day = HomeHistoryDateRange("2026-10-16", "2026-10-16")
        reader.page(2, 100, null).getOrThrow()
        reader.page(1, 20, day).getOrThrow()
        val month = HomeHistoryDateRange.month(HistoryMonth(2024, 2))
        assertEquals(2, reader.calendar(month).getOrThrow().single().brewCount)
        assertEquals(listOf(HomeReadTestRepository.Query(2, 100, null, null),
            HomeReadTestRepository.Query(1, 20, day.from, day.to),
            HomeReadTestRepository.Query(1, 100, month.from, month.to),
            HomeReadTestRepository.Query(2, 100, month.from, month.to)), source.queries)
        assertEquals(listOf("2024-02-01" to "2024-02-29"), source.ranges)
    }

    @Test fun calendarPhotosReadAllMonthPagesPreferCutoutsAndDeduplicateRecords() = runTest {
        val source = HomeReadTestRepository().apply {
            days = { _, _ -> Result.success(listOf(com.coffeepeek.feature.homecheckin.domain.HomeCalendarDay("2026-10-16", 3))) }
            history = { query -> Result.success(HomeCheckInPage(
                if (query.page == 1) listOf(homeReadFixture()) else listOf(
                    homeReadFixture(), homeReadFixture().copy(id = "second", drinkPhotoUrl = null),
                    homeReadFixture().copy(id = "outside", preparedOn = "2026-09-30"),
                ), 101, query.page, query.size,
            )) }
        }
        val range = HomeHistoryDateRange("2026-10-01", "2026-10-31")
        val photos = HomeHistoryReaderAdapter(LegacyHomePreparationReader(source)).calendar(range).getOrThrow().single().photos
        assertEquals(listOf(homeReadFixture().drinkPhotoUrl, homeReadFixture().photos.single().url), photos.map { it.url })
        assertEquals(listOf(true, false), photos.map { it.isCutout })
        assertEquals(List(2) { homeReadFixture().preparedAtUtc }, photos.map { it.occurredAt })
        assertEquals(listOf(1, 2), source.queries.map { it.page })
        assertTrue(source.queries.all { it.size == 100 && it.from == range.from && it.to == range.to })
    }

    @Test fun failedMonthPhotoPageDoesNotPublishPartialCalendarAndCancellationEscapes() = runTest {
        val failure = IllegalStateException("Month photos unavailable")
        val source = HomeReadTestRepository().apply { history = { Result.failure(failure) } }
        val reader = HomeHistoryReaderAdapter(LegacyHomePreparationReader(source))
        val range = HomeHistoryDateRange("2026-10-01", "2026-10-31")
        assertSame(failure, reader.calendar(range).exceptionOrNull())
        val cancellation = CancellationException("Canceled photo page")
        source.history = { Result.failure(cancellation) }
        assertSame(cancellation, assertFailsWith<CancellationException> { reader.calendar(range) })
    }

    @Test fun missingRecipeAndPhotosStayMissingRatherThanInventingDefaults() = runTest {
        val source = HomeReadTestRepository().apply { history = { query ->
            Result.success(HomeCheckInPage(listOf(homeReadFixture().copy(text = null, brewMethodCode = null,
                coffeeProductId = null, beanName = null, doseGrams = null, brewDurationSeconds = null, tasteScore = null,
                drinkName = null, drinkSlug = null, customDrinkName = null, photos = emptyList(), drinkPhotoUrl = null)),
                1, query.page, query.size))
        } }
        val item = HomeHistoryReaderAdapter(LegacyHomePreparationReader(source)).page(1, 20, null).getOrThrow().items.single()
        assertNull(item.recipe.doseGrams)
        assertNull(item.recipe.tasteScore)
        assertNull(item.recipe.coffeeProductId)
        assertNull(item.drinkName)
        assertNull(item.drinkPhotoUrl)
        assertNull(item.note)
        assertTrue(item.photos.isEmpty())
    }

    @Test fun invalidPagingNeverInvokesClampingLegacyTransportAndMalformedMetadataFails() = runTest {
        val source = HomeReadTestRepository()
        val reader = HomeHistoryReaderAdapter(LegacyHomePreparationReader(source))
        for ((page, size) in listOf(0 to 20, 10001 to 20, 1 to 0, 1 to 101)) {
            assertTrue(reader.page(page, size, null).isFailure)
        }
        assertTrue(source.queries.isEmpty())
        source.history = { Result.success(HomeCheckInPage(emptyList(), 0, 3, 20)) }
        assertTrue(reader.page(1, 20, null).isFailure)
        source.days = { _, _ -> Result.success(listOf(com.coffeepeek.feature.homecheckin.domain.HomeCalendarDay("2026-02-30", 1))) }
        assertTrue(reader.calendar(HomeHistoryDateRange("2026-02-01", "2026-02-28")).isFailure)
    }

    @Test fun failuresKeepTheirIdentityAndThrownOrResultWrappedCancellationIsRethrown() = runTest {
        val source = HomeReadTestRepository()
        val reader = HomeHistoryReaderAdapter(LegacyHomePreparationReader(source))
        val range = HomeHistoryDateRange("2026-10-16", "2026-10-16")
        val failure = IllegalStateException("Unavailable")
        source.history = { Result.failure(failure) }
        source.days = { _, _ -> Result.failure(failure) }
        assertSame(failure, reader.page(1, 20, null).exceptionOrNull())
        assertSame(failure, reader.calendar(range).exceptionOrNull())
        val cancellation = CancellationException("Canceled")
        source.history = { Result.failure(cancellation) }
        source.days = { _, _ -> throw cancellation }
        assertSame(cancellation, assertFailsWith<CancellationException> { reader.page(1, 20, null) })
        assertSame(cancellation, assertFailsWith<CancellationException> { reader.calendar(range) })
    }

    @Test fun callerCancellationIsNotConvertedIntoALateOrdinaryFailure() = runTest {
        val gate = CompletableDeferred<Unit>()
        val source = HomeReadTestRepository().apply { history = {
            withContext(NonCancellable) {
                gate.await()
                Result.failure(IllegalStateException("Late transport error"))
            }
        } }
        val reader = HomeHistoryReaderAdapter(LegacyHomePreparationReader(source))
        var returnedNormally = false
        val operation = async { reader.page(1, 20, null); returnedNormally = true }
        runCurrent()
        operation.cancel()
        gate.complete(Unit)
        assertFailsWith<CancellationException> { operation.await() }
        assertFalse(returnedNormally)
    }
}
