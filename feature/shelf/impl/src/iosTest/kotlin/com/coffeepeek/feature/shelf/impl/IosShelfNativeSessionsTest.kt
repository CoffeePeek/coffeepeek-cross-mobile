package com.coffeepeek.feature.shelf.impl

import com.coffeepeek.feature.shelf.api.ShelfCoffeeSearch
import com.coffeepeek.feature.shelf.domain.model.BagScannerReadiness
import com.coffeepeek.feature.shelf.impl.ui.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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

    @Test fun nativePhotoWaitsForModelsThenSavesOnlyTheProcessedCard() = runTest(dispatcher) {
        val repository = FakeShelfRepository(emptyList())
        val scanner = FakeBagScanner(BagScannerReadiness.NeedsDownload(100))
        val search = ShelfCoffeeSearch { Result.success(emptyList()) }
        val session = IosShelfAddSession(ShelfAddViewModel(repository, scanner, search, ShelfArrivals(), testCalendar()))
        val outcomes = mutableListOf<Boolean>()
        val observer = session.events { outcomes += it }
        try {
            runCurrent()
            session.action("camera")
            runCurrent()
            session.action("closeCamera")
            runCurrent()
            assertEquals("Intro", session.current().step)
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
            assertEquals("Form", session.current().step)
            session.query("Custom coffee")
            runCurrent()
            val saveGate = CompletableDeferred<Unit>()
            repository.gate = { saveGate.await() }
            session.save(false)
            session.save(false)
            runCurrent()
            saveGate.complete(Unit)
            runCurrent()
            assertEquals(1, repository.added.size)
            assertEquals(listOf(true), outcomes)
        } finally { session.close() }
        assertTrue(observer.isCancelled)
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
}
