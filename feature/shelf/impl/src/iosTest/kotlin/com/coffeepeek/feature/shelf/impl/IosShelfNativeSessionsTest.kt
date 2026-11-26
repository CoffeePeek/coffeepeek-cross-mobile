package com.coffeepeek.feature.shelf.impl

import com.coffeepeek.feature.shelf.api.ShelfCoffeeSearch
import com.coffeepeek.feature.shelf.domain.model.BagScannerReadiness
import com.coffeepeek.feature.shelf.domain.model.PublicShelf
import com.coffeepeek.feature.shelf.domain.model.PublicShelfItem
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.ui.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class IosShelfNativeSessionsTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() { Dispatchers.resetMain() }

    @Test fun nativeShelfPreservesHandoffFinishAndUndoPosition() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("first"), bag("second")))
        val handoff = ShelfBagHandoff()
        val session = IosShelfSession(ShelfViewModel(repository, testCalendar(), ShelfArrivals(), handoff))
        val events = mutableListOf<String>()
        val observer = session.events { kind, id -> events += "$kind:$id" }
        try {
            runCurrent()
            session.open("second")
            runCurrent()
            assertEquals("second", handoff.opened("second")?.id)
            assertEquals(listOf("bag:second"), events)
            session.finish("second", 4, true)
            runCurrent()
            assertTrue(session.current().undo)
            session.undo()
            runCurrent()
            assertEquals(listOf("first", "second"), session.current().items.map { it.id })
            assertTrue(repository.calls.contains("restore:second:1"))
        } finally { session.close() }
        assertTrue(observer.isCancelled)
    }

    @Test fun nativeFinishDefaultsToFiveAndResetsBeforeSkip() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("first")))
        val session = IosShelfSession(ShelfViewModel(repository, testCalendar(), ShelfArrivals(), ShelfBagHandoff()))
        try {
            runCurrent()
            session.startFinish("first")
            runCurrent()
            assertEquals(5, session.current().finishRating)
            assertNull(session.current().finishBuyAgain)
            finishWithSelectedChoices(session, repository)
            session.undo()
            runCurrent()
            assertResetAndSkip(session, repository)
        } finally { session.close() }
    }

    private fun TestScope.finishWithSelectedChoices(session: IosShelfSession, repository: FakeShelfRepository) {
        session.rate(5)
        assertEquals(0, session.current().finishRating)
        session.rate(3)
        session.chooseAgain(true)
        session.chooseAgain(true)
        assertNull(session.current().finishBuyAgain)
        session.chooseAgain(false)
        session.confirmFinish("first", false)
        runCurrent()
        assertEquals("finish:first:3:false", repository.calls.last())
    }

    private fun TestScope.assertResetAndSkip(session: IosShelfSession, repository: FakeShelfRepository) {
        session.startFinish("first")
        runCurrent()
        assertEquals(5, session.current().finishRating)
        assertNull(session.current().finishBuyAgain)
        session.confirmFinish("first", true)
        runCurrent()
        assertEquals("finish:first:null:null", repository.calls.last())
    }

    @Test fun nativeBagFinishUsesSameDefaultAndCancelResetsIt() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("first")))
        val model = ShelfBagViewModel("first", repository, testCalendar(), ShelfBagHandoff(), FakeShelfBrewLog())
        val session = IosShelfBagSession(model)
        try {
            runCurrent()
            session.action("finish")
            runCurrent()
            assertEquals(5, session.current().finishRating)
            cancelBagFinishThenReopen(session)
            session.confirmFinish(false)
            runCurrent()
            assertEquals("finish:first:5:null", repository.calls.last())
        } finally { session.close() }
    }

    private fun TestScope.cancelBagFinishThenReopen(session: IosShelfBagSession) {
        session.rate(2)
        session.chooseAgain(true)
        session.action("cancelFinish")
        runCurrent()
        session.action("finish")
        runCurrent()
        assertEquals(5, session.current().finishRating)
        assertNull(session.current().finishBuyAgain)
    }

    @Test fun oldFeedbackExpiryCannotDismissANewerUndoForTheSameBag() = runTest(dispatcher) {
        val repository = FakeShelfRepository(listOf(bag("first")))
        val session = IosShelfSession(ShelfViewModel(repository, testCalendar(), ShelfArrivals(), ShelfBagHandoff()))
        try {
            runCurrent()
            session.finish("first", 5, null)
            runCurrent()
            val expired = assertNotNull(session.current().messageKey)
            session.undo()
            runCurrent()
            session.finish("first", 5, null)
            runCurrent()
            assertFeedbackCorrelation(session, expired)
        } finally { session.close() }
    }

    private fun TestScope.assertFeedbackCorrelation(session: IosShelfSession, expired: String) {
        val current = assertNotNull(session.current().messageKey)
        assertNotEquals(expired, current)
        session.dismissMessage(expired)
        runCurrent()
        assertTrue(session.current().undo)
        session.dismissMessage(current)
        runCurrent()
        assertFalse(session.current().undo)
    }

    @Test fun visibilitySnapshotKeepsLoadAndSaveFailuresDistinct() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply {
            visibilityRead = { Result.failure(IllegalStateException("offline")) }
        }
        val session = createIosShelfVisibilitySession(repository)
        try {
            runCurrent()
            assertEquals("Load", session.current().error)
            repository.visibilityRead = { Result.success(false) }
            session.refresh()
            runCurrent()
            assertNull(session.current().error)
            repository.visibilityWrite = { Result.failure(IllegalStateException("offline")) }
            session.set(true)
            runCurrent()
            assertEquals("Save", session.current().error)
            assertFalse(session.current().visible)
        } finally { session.close() }
    }

    @Test fun nativePhotoWaitsForModelsThenSavesOnlyTheProcessedCard() = runTest(dispatcher) {
        val repository = FakeShelfRepository(emptyList())
        val scanner = FakeBagScanner(BagScannerReadiness.NeedsDownload(100))
        val search = ShelfCoffeeSearch { Result.success(emptyList()) }
        val session = IosShelfAddSession(ShelfAddViewModel(repository, scanner, search, ShelfArrivals(), testCalendar()))
        val outcomes = mutableListOf<Boolean>()
        val observer = session.events { outcomes += it }
        try {
            runCurrent()
            cameraBackReturnsToIntro(session)
            prepareSelectedPhoto(session, scanner)
            assertEquals("Form", session.current().step)
            session.query("Custom coffee")
            runCurrent()
            saveWhileBusy(session, repository)
            assertEquals(1, repository.added.size)
            assertEquals(listOf(true), outcomes)
        } finally { session.close() }
        assertTrue(observer.isCancelled)
    }

    private fun TestScope.cameraBackReturnsToIntro(session: IosShelfAddSession) {
        session.action("camera")
        runCurrent()
        session.action("closeCamera")
        runCurrent()
        assertEquals("Intro", session.current().step)
    }

    private fun TestScope.prepareSelectedPhoto(session: IosShelfAddSession, scanner: FakeBagScanner) {
        session.photo(byteArrayOf(9, 8), false)
        runCurrent()
        assertEquals("Preparing", session.current().step)
        assertEquals(0, scanner.scans)
        session.action("download")
        runCurrent()
        assertEquals(1, scanner.scans)
        assertContentEquals(byteArrayOf(9, 8), scanner.scannedPhotos.single())
        session.action("revealed")
        runCurrent()
    }

    private fun TestScope.saveWhileBusy(session: IosShelfAddSession, repository: FakeShelfRepository) {
        val gate = CompletableDeferred<Unit>()
        repository.gate = { gate.await() }
        session.save(false)
        session.save(false)
        runCurrent()
        gate.complete(Unit)
        runCurrent()
    }

    @Test fun nativePublicShelfNeverOpensAnUnlinkedBagOrAfterProjectionIsHidden() = runTest(dispatcher) {
        val repository = FakeShelfProfileRepository().apply { publicRead = { Result.success(profileShelf(null)) } }
        val session = createIosShelfProfileSession(repository, "public-person", false)
        val coffees = mutableListOf<String>()
        val observer = session.events { coffees += it }
        try {
            runCurrent()
            assertTrue(session.current().visible)
            session.open("bag")
            runCurrent()
            assertTrue(coffees.isEmpty())
            repository.publicRead = { Result.success(null) }
            session.refresh()
            runCurrent()
            assertFalse(session.current().visible)
            session.open("bag")
            runCurrent()
            assertTrue(coffees.isEmpty())
        } finally { session.close() }
        assertTrue(observer.isCancelled)
    }

    @Test fun publicPhotoFinishingAfterAccountReplacementIsCleared() = runTest(dispatcher) {
        val source = FakeShelfProfileRepository().apply {
            publicRead = { Result.success(PublicShelf(listOf(PublicShelfItem("bag", "Coffee", null, null, "public-card")), 1)) }
        }
        val gate = CompletableDeferred<Unit>()
        val bytes = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8)
        val repository = object : ShelfProfileRepository by source {
            override suspend fun photo(url: String): Result<ByteArray> { gate.await(); return Result.success(bytes) }
        }
        val session = createIosShelfProfileSession(repository, "public-person", false)
        try {
            runCurrent()
            val request = async { session.photo("bag") }
            runCurrent()
            source.publicRead = { Result.success(null) }
            source.owners.value = "replacement"
            runCurrent()
            gate.complete(Unit)
            runCurrent()
            assertNull(request.await())
            assertTrue(bytes.all { it == 0.toByte() })
        } finally { session.close() }
    }
}
