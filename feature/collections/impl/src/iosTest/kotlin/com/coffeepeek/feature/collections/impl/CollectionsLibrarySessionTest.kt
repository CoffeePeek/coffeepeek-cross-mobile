package com.coffeepeek.feature.collections.impl

import com.coffeepeek.feature.collections.api.CollectionsLibraryEvent
import com.coffeepeek.feature.collections.api.CollectionsLibrarySession
import com.coffeepeek.feature.collections.domain.model.SavedCollectionCover
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.SavedTestAccount
import com.coffeepeek.feature.collections.impl.ui.SavedTestRepository
import com.coffeepeek.feature.collections.impl.ui.SavedTestSnapshots
import com.coffeepeek.feature.collections.impl.ui.privateList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsLibrarySessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val saved = SavedTestRepository()
    private val account = SavedTestAccount()
    private val sessions = mutableListOf<CollectionsLibrarySession>()

    @BeforeTest fun before() {
        Dispatchers.setMain(dispatcher)
        saved.currentOwner = account::currentId
    }

    @AfterTest fun after() {
        sessions.forEach(CollectionsLibrarySession::close)
        Dispatchers.resetMain()
    }

    @Test fun snapshotReplaysAndCancelledObserverStopsReceivingChanges() = runTest(dispatcher) {
        val session = session()
        val queries = mutableListOf<String>()
        val observation = session.observe({ queries.add(it.query) }, {})
        assertTrue(queries.isEmpty())
        runCurrent()
        assertEquals(listOf("private", "public"), session.current().items.map { it.id })
        assertEquals(listOf(false, true), session.current().items.map { it.isPublic })
        session.query("Public")
        runCurrent()
        assertEquals("Public", queries.last())
        observation.cancel()
        session.query("Private")
        runCurrent()
        assertEquals("Public", queries.last())
    }

    @Test fun switchingAccountClearsPrivateItemsAndSearch() = runTest(dispatcher) {
        val session = session()
        runCurrent()
        val version = session.current().accountVersion
        session.query("Private")
        runCurrent()
        account.owner.value = null
        runCurrent()
        assertFalse(session.current().signedIn)
        assertTrue(session.current().items.isEmpty())
        assertEquals("", session.current().query)
        assertTrue(session.current().accountVersion > version)
    }

    @Test fun editorAndSignInKeepExistingNavigationEvents() = runTest(dispatcher) {
        val session = session()
        val events = mutableListOf<CollectionsLibraryEvent>()
        session.observe({}, events::add)
        runCurrent()
        session.openEditor("private")
        session.openEditor("missing")
        session.openEditor(null)
        runCurrent()
        assertEquals(listOf(CollectionsLibraryEvent.Editor("private"), CollectionsLibraryEvent.Editor(null)), events)
        account.owner.value = null
        runCurrent()
        session.openEditor(null)
        session.signIn()
        runCurrent()
        assertEquals(CollectionsLibraryEvent.SignIn, events.last())
        assertEquals(3, events.size)
    }

    @Test fun closeCancelsPendingLoadAndPreventsFurtherActions() = runTest(dispatcher) {
        saved.mineGate = CompletableDeferred()
        val session = session()
        runCurrent()
        assertTrue(session.current().loading)
        session.close()
        saved.mineGate?.complete(Unit)
        session.reload()
        session.query("Ignored")
        runCurrent()
        assertTrue(session.current().items.isEmpty())
        assertEquals("", session.current().query)
        assertEquals(1, saved.mineReads)
    }

    @Test fun privateCoverUsesRepositoryAndErasesTemporaryBytes() = runTest(dispatcher) {
        val bytes = byteArrayOf(2, 4, 6)
        saved.lists = listOf(privateList.summary.copy(cover = SavedCollectionCover("cover", true)))
        val repository = object : SavedCollectionsRepository by saved {
            override suspend fun cover(collectionId: String, cover: SavedCollectionCover) = Result.success(bytes)
        }
        val session = session(repository)
        runCurrent()
        var delivered: ByteArray? = null
        session.loadCover("private", session.current().accountVersion) { value, url ->
            delivered = value?.copyOf()
            assertNull(url)
        }
        runCurrent()
        assertContentEquals(byteArrayOf(2, 4, 6), delivered)
        assertContentEquals(byteArrayOf(0, 0, 0), bytes)
        assertEquals(0, saved.publicReads + saved.ownedReads)
    }

    @Test fun staleCoverCannotReachNewAccountEvenIfLoaderIgnoresCancellation() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val bytes = byteArrayOf(2, 4, 6)
        saved.lists = listOf(privateList.summary.copy(cover = SavedCollectionCover("cover", true)))
        val repository = delayedCoverRepository(gate, bytes)
        val session = session(repository)
        runCurrent()
        var calls = 0
        session.loadCover("private", session.current().accountVersion) { _, _ -> calls++ }
        runCurrent()
        account.owner.value = null
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(0, calls)
        assertContentEquals(byteArrayOf(0, 0, 0), bytes)
    }

    private fun delayedCoverRepository(gate: CompletableDeferred<Unit>, bytes: ByteArray) =
        object : SavedCollectionsRepository by saved {
            override suspend fun cover(collectionId: String, cover: SavedCollectionCover): Result<ByteArray> =
                withContext(NonCancellable) {
                    gate.await()
                    Result.success(bytes)
                }
        }

    private fun session(repository: SavedCollectionsRepository = saved): CollectionsLibrarySession =
        createCollectionsLibrarySession(repository, SavedTestSnapshots(), account).also(sessions::add)
}
