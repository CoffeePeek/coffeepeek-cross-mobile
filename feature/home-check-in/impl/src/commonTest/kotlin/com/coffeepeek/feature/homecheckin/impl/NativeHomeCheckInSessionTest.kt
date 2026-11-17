package com.coffeepeek.feature.homecheckin.impl

import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class NativeHomeCheckInSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessions = mutableListOf<NativeHomeCheckInSession>()
    private var conversions = 0
    private var allowed = true
    private val dates = HomeCheckInEditorDates({ "2026-10-17T22:30:00Z" }, { "2026-10-18" }, { "date:$it" },
        { conversions++; "${it}T09:00:00Z" }, { "method:$it" })

    @BeforeTest fun setup() = Dispatchers.setMain(dispatcher)
    @AfterTest fun teardown() {
        sessions.forEach { it.close() }
        Dispatchers.resetMain()
    }

    private fun session(repository: Repository = Repository(), id: String? = null, bean: HomeCheckInBean? = null) =
        NativeHomeCheckInSession(repository, Catalog(), dates, { allowed }, id, bean = bean).also { sessions += it }

    @Test fun nativeSnapshotUsesLocalDateAndExistingFieldRules() = runTest(dispatcher) {
        val editor = session(bean = HomeCheckInBean("coffee", "Bean", "bag"))
        runCurrent()
        assertEquals("2026-10-18", editor.today())
        assertEquals("date:2026-10-18", editor.current().draft.dateLabel)
        assertEquals("bag", editor.current().draft.shelfBagId)
        assertEquals(5, editor.current().draft.tasteScore)
        editor.doseChanged("18,5")
        editor.noteChanged("x".repeat(1005))
        editor.tasteTapped(5)
        runCurrent()
        assertEquals("18.5", editor.current().draft.dose)
        assertEquals(1000, editor.current().draft.note.length)
        assertNull(editor.current().draft.tasteScore)
    }

    @Test fun selectingSameCivilDatePreservesExactEditTimestampAndNullableVisibility() = runTest(dispatcher) {
        val repository = Repository()
        val editor = session(repository, "home")
        runCurrent()
        editor.dateChanged("2026-10-16")
        editor.save()
        runCurrent()
        assertEquals(0, conversions)
        assertEquals("2026-10-16T07:30:00Z", repository.writes.single().preparedAt)
        assertNull(editor.current().saved?.isPublic)
        assertFalse(editor.current().status.canAddPhotos)
    }

    @Test fun preparedPhotoDeliveryIsBoundedOwnedAndSingleUse() = runTest(dispatcher) {
        val editor = session()
        runCurrent()
        val bytes = byteArrayOf(1, 2)
        val request = editor.beginPhotos(2)
        editor.deliverPhotos(request, List(3) { NativeHomeCheckInPhoto("$it.jpg", "image/jpeg", bytes) })
        bytes[0] = 99
        editor.deliverPhotos(request, listOf(photo()))
        runCurrent()
        assertEquals(2, editor.current().draft.photos.size)
        assertEquals(1.toByte(), editor.current().draft.photos.first().bytes[0])
        editor.current().draft.photos.first().bytes[0] = 88
        assertEquals(1.toByte(), editor.current().draft.photos.first().bytes[0])
        assertFalse(editor.current().status.photosPreparing)
    }

    @Test fun cancelledPhotoRequestDoesNotBlockSaveAndLateCallbackCannotReplaceNextRequest() = runTest(dispatcher) {
        val editor = session()
        runCurrent()
        val first = editor.beginPhotos()
        editor.deliverPhotos(first, emptyList())
        assertTrue(editor.current().status.canAddPhotos)
        val second = editor.beginPhotos()
        editor.deliverPhotos(first, listOf(photo()))
        assertTrue(editor.current().status.photosPreparing)
        editor.deliverPhotos(second, listOf(photo()))
        runCurrent()
        assertEquals(1, editor.current().draft.photos.size)
    }

    @Test fun pendingPhotoPreventsSubmitAndRevokedOwnerDropsPreparedBytes() = runTest(dispatcher) {
        val repository = Repository()
        val editor = session(repository)
        runCurrent()
        val request = editor.beginPhotos()
        editor.save()
        runCurrent()
        assertTrue(repository.writes.isEmpty())
        allowed = false
        editor.deliverPhotos(request, listOf(photo()))
        editor.revokeAccess()
        runCurrent()
        assertTrue(editor.current().status.revoked)
        assertTrue(editor.current().draft.photos.isEmpty())
        assertFalse(editor.isCurrent())
    }

    @Test fun acceptedSaveWaitsForConfirmationAndDeliversExactlyOneOutcome() = runTest(dispatcher) {
        val editor = session()
        val events = mutableListOf<NativeHomeCheckInEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { editor.events.collect { events += it } }
        runCurrent()
        editor.save()
        runCurrent()
        assertNotNull(editor.current().saved)
        assertTrue(events.isEmpty())
        editor.finishSaved()
        editor.finishSaved()
        runCurrent()
        assertEquals(listOf(NativeHomeCheckInEvent("saved", "home", true)), events)
    }

    @Test fun closingNativeOwnerCancelsPendingDetailAndSuppressesPhotoDelivery() = runTest(dispatcher) {
        val repository = Repository().apply { detailGate = CompletableDeferred() }
        val editor = session(repository, "home")
        runCurrent()
        editor.close()
        runCurrent()
        assertTrue(repository.detailCancelled)
        assertTrue(editor.current().status.closed)
        assertEquals(0L, editor.beginPhotos())
    }

    private class Catalog : HomeCheckInEditorCatalog {
        override suspend fun brewMethods() = Result.success(listOf(HomeCheckInBrewMethod("v60", "V60")))
        override suspend fun searchBeans(query: String) = Result.success(emptyList<HomeCheckInBeanOption>())
    }

    private class Repository : HomePreparationRepository {
        val writes = mutableListOf<HomePreparationInput>()
        var detailGate: CompletableDeferred<Unit>? = null
        var detailCancelled = false
        override suspend fun detail(id: String): Result<HomePreparation> {
            try { detailGate?.await() } catch (cancelled: CancellationException) {
                detailCancelled = true
                throw cancelled
            }
            return Result.success(entry())
        }
        override suspend fun create(input: HomePreparationInput) = write(input)
        override suspend fun update(id: String, input: HomePreparationInput) = write(input)
        private fun write(input: HomePreparationInput): Result<HomePreparation> {
            writes += input
            return Result.success(entry())
        }
        override suspend fun delete(id: String): Result<Unit> = error("No delete")
        override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> = error("No history")
        override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> = error("No calendar")
    }

    companion object {
        private fun entry() = HomePreparation("home", "2026-10-16", "2026-10-16T07:30:00Z", "2026-10-16T07:30:00Z",
            "note", "v60", null, "Bean", "18", 180, 2, null, null, null,
            emptyList(), null, HomePreparationModeration.Unknown)
        private fun photo() = NativeHomeCheckInPhoto("photo.jpg", "image/jpeg", byteArrayOf(1))
    }
}
