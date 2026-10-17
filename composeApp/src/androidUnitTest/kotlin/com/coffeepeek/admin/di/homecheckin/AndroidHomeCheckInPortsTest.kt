package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistorySession
import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.BrewMethodOption
import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.feature.coffee.api.CoffeePickerItem
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.model.Session
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidHomeCheckInPortsTest {
    @Test fun catalogMapsActualSlugsLabelsAndHistoricalAliasesWithoutASecondBackend() = runTest {
        val lease = AndroidCheckInHistorySession(CheckInTestSessions())
        val queries = mutableListOf<String>()
        val catalog = AndroidHomeCheckInCatalog(lease, object : CoffeePicker {
            override suspend fun search(query: String): Result<List<CoffeePickerItem>> {
                queries += query; return Result.success(listOf(CoffeePickerItem("slug", "Зерно", "Обжарщик")))
            }
        }, BrewMethodCatalog { Result.success(listOf(BrewMethodOption("hario-v60", "Hario V60"))) })
        assertEquals("slug", catalog.searchBeans("query").getOrThrow().single().slug)
        assertEquals(listOf("query"), queries)
        val method = catalog.brewMethods().getOrThrow().single()
        assertEquals("hario-v60", method.code); assertEquals("Hario V60", method.name)
        assertEquals(setOf("pour_over"), method.legacyCodes)
    }

    @Test fun extractionOwnsBothByteBoundariesAndDoesNotSwallowCancellation() = runTest {
        val original = HomePreparationUpload("original", "image/jpeg", byteArrayOf(1))
        val candidate = PendingPhotoUpload("cutout", "image/png", byteArrayOf(2))
        val lease = AndroidCheckInHistorySession(CheckInTestSessions())
        val processor = AndroidHomeCheckInPhotoProcessor(lease, DrinkPhotoExtractor {
            it.bytes[0] = 0; listOf(candidate)
        })
        val result = processor.process(original).getOrThrow().single()
        candidate.bytes[0] = 0
        assertContentEquals(byteArrayOf(1), original.bytes)
        assertContentEquals(byteArrayOf(2), result.bytes)
        assertEquals("image/png", result.contentType)
        val cancelled = AndroidHomeCheckInPhotoProcessor(lease, DrinkPhotoExtractor { throw CancellationException("cancelled") })
        assertFailsWith<CancellationException> { cancelled.process(original) }
    }

    @Test fun accountReplacementRejectsLateExtractionEvenIfItsImplementationIgnoresCancellation() = runTest {
        val sessions = CheckInTestSessions()
        val lease = AndroidCheckInHistorySession(sessions)
        val gate = CompletableDeferred<Unit>()
        val processor = AndroidHomeCheckInPhotoProcessor(lease, DrinkPhotoExtractor {
            withContext(NonCancellable) { gate.await() }
            listOf(PendingPhotoUpload("private", "image/png", byteArrayOf(1)))
        })
        val result = async { processor.process(HomePreparationUpload("source", "image/jpeg", byteArrayOf(1))) }
        runCurrent(); sessions.applySession(Session("other", userId = "other")); runCurrent()
        gate.complete(Unit)
        assertFailsWith<CancellationException> { result.await() }
        assertFalse(lease.isCurrent())
    }

    @Test fun repositoryKeepsResultsAndTokenRenewalButRevokedLeaseNeverStartsAnotherCall() = runTest {
        val sessions = CheckInTestSessions()
        val lease = AndroidCheckInHistorySession(sessions)
        val source = Repository()
        val repository = AndroidHomeCheckInRepository(lease, source)
        sessions.applySession(Session("renewed", userId = "owner"))
        assertTrue(repository.delete("id").isSuccess)
        val failure = repository.detail("id").exceptionOrNull()
        assertSame(source.failure, failure)
        assertTrue(repository.page(1, 10, null).isSuccess)
        assertTrue(repository.calendar(HomePreparationDateRange("2026-10-01", "2026-10-17")).isSuccess)
        assertSame(source.failure, repository.create(HomePreparationInput("2026-10-17T07:00:00Z")).exceptionOrNull())
        assertSame(source.failure, repository.update("id", HomePreparationInput("2026-10-17T07:00:00Z")).exceptionOrNull())
        assertEquals(6, source.calls)
        sessions.applySession(null)
        assertFailsWith<CancellationException> { repository.delete("id") }
        assertEquals(6, source.calls)
    }

    @Test fun guestCatalogueAndProcessorDoNotInvokeActualPortsAndPickedOriginalsAreBounded() = runTest {
        val lease = AndroidCheckInHistorySession(CheckInTestSessions(null))
        val catalog = AndroidHomeCheckInCatalog(lease, object : CoffeePicker {
            override suspend fun search(query: String): Result<List<CoffeePickerItem>> = error("Guest search")
        }, BrewMethodCatalog { error("Guest methods") })
        val processor = AndroidHomeCheckInPhotoProcessor(lease, DrinkPhotoExtractor { error("Guest extraction") })
        assertTrue(catalog.brewMethods().isFailure); assertTrue(catalog.searchBeans("").isFailure)
        assertTrue(processor.process(HomePreparationUpload("photo", "image/jpeg", byteArrayOf(1))).isFailure)
        val bytes = byteArrayOf(2)
        val photos = (1..7).map { PickedImage(bytes, it.toString()) }
        val selected = photos.toHomePreparationPhotos(99)
        assertEquals(5, selected.size)
        assertEquals(2, photos.toHomePreparationPhotos(2).size)
        assertTrue(photos.toHomePreparationPhotos(-1).isEmpty())
        bytes[0] = 0
        assertContentEquals(byteArrayOf(2), selected.first().bytes)
    }

    @Test fun dateAssemblyPreservesCivilDateAndLimitsInitialTimestampPrecision() {
        val dates = androidHomeCheckInDates()
        val instant = dates.currentInstant()
        assertTrue(Regex("""\.\d{1,3}Z$|:\d{2}Z$""").containsMatchIn(instant))
        assertEquals("2026-10-16", dates.localDate(dates.civilDateToInstant("2026-10-16")))
        assertEquals("16 октября 2026", dates.formatDate("2026-10-16"))
        assertEquals("Пуровер", dates.formatMethod("pour_over"))
    }

    @Test fun catalogueTransportMessagesStayHiddenAndReturnedCancellationIsRethrown() = runTest {
        val lease = AndroidCheckInHistorySession(CheckInTestSessions())
        val catalog = AndroidHomeCheckInCatalog(lease, object : CoffeePicker {
            override suspend fun search(query: String): Result<List<CoffeePickerItem>> =
                Result.failure(java.io.IOException("Private transport details"))
        }, BrewMethodCatalog { Result.failure(CancellationException("cancelled")) })
        val failure = catalog.searchBeans("").exceptionOrNull() as HomePreparationFailure
        assertEquals(HomePreparationIssue.Offline, failure.issue)
        assertFalse(failure.message.orEmpty().contains("Private transport details"))
        assertFailsWith<CancellationException> { catalog.brewMethods() }
    }

    private class Repository : HomePreparationRepository {
        val failure = HomePreparationFailure(HomePreparationIssue.NotFound)
        var calls = 0
        override suspend fun detail(id: String): Result<HomePreparation> { calls++; return Result.failure(failure) }
        override suspend fun create(input: HomePreparationInput): Result<HomePreparation> { calls++; return Result.failure(failure) }
        override suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation> { calls++; return Result.failure(failure) }
        override suspend fun delete(id: String): Result<Unit> { calls++; return Result.success(Unit) }
        override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> {
            calls++; return Result.success(HomePreparationPage(emptyList(), 0, page, pageSize))
        }
        override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> {
            calls++; return Result.success(emptyList())
        }
    }
}
