package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.di.homecheckin.HomePreparationTestReader
import com.coffeepeek.admin.di.homecheckin.homeReadFixture
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryDateRange
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationModeration
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationPage
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationVisibility
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationCalendarPhoto
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationDay
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
        val source = HomePreparationTestReader().apply {
            history = { query -> Result.success(HomePreparationPage(listOf(homeReadFixture().copy(
                photos = listOf(com.coffeepeek.feature.homecheckin.domain.model.HomePreparationPhoto(
                    "photo", "original", "feed", "detail", "thumb")))), 1, query.page, query.size)) }
        }
        val entry = HomeHistoryReaderAdapter(source).page(1, 20, null).getOrThrow().items.single()
        assertEquals("original", entry.photos.single().url)
        assertEquals("feed", entry.photos.single().thumbnailUrl)
        assertEquals("detail", entry.photos.single().detailUrl)
        assertEquals("thumb", entry.photos.single().calendarUrl)
    }

    @Test fun snapshotsPreserveCivilDateRecipePhotoIdentityAndUnknownPublication() = runTest {
        val source = homeReadFixture()
        val reader = HomeHistoryReaderAdapter(HomePreparationTestReader())
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
        assertNull(item.visibility)
        assertEquals("Unknown", item.moderation.name)
        assertTrue(page.hasMore)
    }

    @Test fun allVisibilityAndModerationStatesSurviveTheFeatureBoundary() = runTest {
        val source = HomePreparationTestReader()
        val reader = HomeHistoryReaderAdapter(source)
        for (visibility in listOf(null) + HomePreparationVisibility.entries) {
            for (moderation in HomePreparationModeration.entries) {
                source.history = { query -> Result.success(HomePreparationPage(
                    listOf(homeReadFixture().copy(visibility = visibility, moderation = moderation)), 1, query.page, query.size)) }
                val item = reader.page(1, 20, null).getOrThrow().items.single()
                assertEquals(visibility?.name, item.visibility?.name)
                assertEquals(moderation.name, item.moderation.name)
            }
        }
    }

    @Test fun pagingAndCalendarPreserveExistingAllDatesSingleDayAndLastDayQueries() = runTest {
        val source = HomePreparationTestReader()
        val reader = HomeHistoryReaderAdapter(source)
        val day = HomeHistoryDateRange("2026-10-16", "2026-10-16")
        reader.page(2, 100, null).getOrThrow()
        reader.page(1, 20, day).getOrThrow()
        val month = HomeHistoryDateRange.month(HistoryMonth(2024, 2))
        assertEquals(2, reader.calendar(month).getOrThrow().single().brewCount)
        assertEquals(listOf(HomePreparationTestReader.Query(2, 100, null, null),
            HomePreparationTestReader.Query(1, 20, day.from, day.to)), source.queries)
        assertEquals(listOf("2024-02-01" to "2024-02-29"), source.ranges)
    }

    @Test fun calendarPreservesAlreadyEnrichedPhotosAndPhotoLessCountsWithoutAdditionalReads() = runTest {
        val fixture = homeReadFixture()
        val source = HomePreparationTestReader().apply {
            days = { _, _ -> Result.success(listOf(
                HomePreparationDay("2026-10-16", 3, listOf(
                    HomePreparationCalendarPhoto("other-issued-photo", false, fixture.preparedAtUtc),
                    HomePreparationCalendarPhoto(fixture.photos.single().url, false, fixture.preparedAtUtc),
                )),
                HomePreparationDay("2026-10-17", 1),
            )) }
        }
        val range = HomeHistoryDateRange("2026-10-01", "2026-10-31")
        val days = HomeHistoryReaderAdapter(source).calendar(range).getOrThrow()
        assertEquals(listOf(3, 1), days.map { it.brewCount })
        assertEquals(listOf("other-issued-photo", fixture.photos.single().url), days.first().photos.map { it.url })
        assertEquals(listOf(false, false), days.first().photos.map { it.isCutout })
        assertEquals(List(2) { fixture.preparedAtUtc }, days.first().photos.map { it.occurredAt })
        assertTrue(days.last().photos.isEmpty())
        assertTrue(source.queries.isEmpty()) // Enrichment belongs to Home data, not this app adapter.
        assertEquals(listOf(range.from to range.to), source.ranges)
    }

    @Test fun failedEnrichmentDoesNotPublishPartialCalendarAndCancellationEscapes() = runTest {
        val failure = IllegalStateException("Month photos unavailable")
        val source = HomePreparationTestReader().apply { days = { _, _ -> Result.failure(failure) } }
        val reader = HomeHistoryReaderAdapter(source)
        val range = HomeHistoryDateRange("2026-10-01", "2026-10-31")
        assertSame(failure, reader.calendar(range).exceptionOrNull())
        val cancellation = CancellationException("Canceled photo page")
        source.days = { _, _ -> Result.failure(cancellation) }
        assertSame(cancellation, assertFailsWith<CancellationException> { reader.calendar(range) })
        assertTrue(source.queries.isEmpty())
    }

    @Test fun missingRecipeAndPhotosStayMissingRatherThanInventingDefaults() = runTest {
        val source = HomePreparationTestReader().apply { history = { query ->
            Result.success(HomePreparationPage(listOf(homeReadFixture().copy(text = null, brewMethodCode = null,
                coffeeProductId = null, beanName = null, doseGrams = null, brewDurationSeconds = null, tasteScore = null,
                drinkName = null, drinkSlug = null, customDrinkName = null, photos = emptyList())),
                1, query.page, query.size))
        } }
        val item = HomeHistoryReaderAdapter(source).page(1, 20, null).getOrThrow().items.single()
        assertNull(item.recipe.doseGrams)
        assertNull(item.recipe.tasteScore)
        assertNull(item.recipe.coffeeProductId)
        assertNull(item.drinkName)
        assertNull(item.note)
        assertTrue(item.photos.isEmpty())
    }

    @Test fun invalidPagingNeverInvokesReaderAndMalformedMetadataFails() = runTest {
        val source = HomePreparationTestReader()
        val reader = HomeHistoryReaderAdapter(source)
        for ((page, size) in listOf(0 to 20, 10001 to 20, 1 to 0, 1 to 101)) {
            assertTrue(reader.page(page, size, null).isFailure)
        }
        assertTrue(source.queries.isEmpty())
        source.history = { Result.success(HomePreparationPage(emptyList(), 0, 3, 20)) }
        assertTrue(reader.page(1, 20, null).isFailure)
    }

    @Test fun failuresKeepTheirIdentityAndThrownOrResultWrappedCancellationIsRethrown() = runTest {
        val source = HomePreparationTestReader()
        val reader = HomeHistoryReaderAdapter(source)
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
        val source = HomePreparationTestReader().apply { history = {
            withContext(NonCancellable) {
                gate.await()
                Result.failure(IllegalStateException("Late transport error"))
            }
        } }
        val reader = HomeHistoryReaderAdapter(source)
        var returnedNormally = false
        val operation = async { reader.page(1, 20, null); returnedNormally = true }
        runCurrent()
        operation.cancel()
        gate.complete(Unit)
        assertFailsWith<CancellationException> { operation.await() }
        assertFalse(returnedNormally)
    }
}
