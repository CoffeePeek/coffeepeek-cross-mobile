package com.coffeepeek.feature.collections.impl

import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsWorkspaceSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val account = SavedTestAccount()
    private val saved = SavedTestRepository()
    private val sessions = mutableListOf<CollectionsWorkspaceSession>()
    private val events = mutableListOf<String>()

    @BeforeTest fun before() {
        Dispatchers.setMain(dispatcher)
        saved.currentOwner = account::currentId
    }

    @AfterTest fun after() {
        sessions.forEach(CollectionsWorkspaceSession::close)
        Dispatchers.resetMain()
    }

    @Test fun nativeEditorPreservesMetadataAndItemOrderWrites() = runTest(dispatcher) {
        val second = savedShop.copy(slug = "second", title = "Second")
        saved.value = publicList.copy(places = listOf(savedShop, second))
        val session = session(CollectionsWorkspaceMode.Editor, "public")
        runCurrent()
        assertTrue(session.current().canPreview)
        session.title("Edited")
        session.description("Description")
        session.movePlace(second.key, savedShop.key, false)
        runCurrent()
        assertFalse(session.current().canPreview)
        session.save()
        runCurrent()
        assertEquals("Edited", session.current().title)
        assertEquals("Description", session.current().description)
        assertEquals(listOf(second.key, savedShop.key), session.current().places.map { it.key })
        assertEquals(1, saved.updates)
        assertEquals(1, saved.itemWrites)
    }

    @Test fun featuredChoiceRemainsPublicAndAccountScoped() = runTest(dispatcher) {
        val session = session(CollectionsWorkspaceMode.Preference)
        runCurrent()
        session.feature("private")
        runCurrent()
        assertEquals(0, saved.featureWrites)
        session.feature(null)
        runCurrent()
        assertEquals(1, saved.featureWrites)
        assertNull(session.current().featuredId)
        account.owner.value = null
        runCurrent()
        assertTrue(session.current().lists.isEmpty())
        session.feature("public")
        runCurrent()
        assertEquals(1, saved.featureWrites)
    }

    @Test fun aCoverFromAnOldPickerCannotEnterTheNextAccount() = runTest(dispatcher) {
        val session = session(CollectionsWorkspaceMode.Editor)
        runCurrent()
        val version = session.current().accountVersion
        account.owner.value = "second"
        runCurrent()
        session.coverPicked(byteArrayOf(1, 2, 3), version)
        session.coverPreparing(true, version)
        runCurrent()
        assertFalse(session.current().hasCover)
        assertFalse(session.current().preparingCover)
    }

    @Test fun posterBackInvalidatesPendingNativeExport() = runTest(dispatcher) {
        saved.value = publicList
        val session = session(CollectionsWorkspaceMode.Editor, "public")
        var epoch = -1
        var poster: CollectionsPoster? = null
        session.observe({}, { value, generation -> poster = value; epoch = generation })
        runCurrent()
        session.preview()
        session.shareImages()
        runCurrent()
        assertEquals(1080, poster?.width)
        assertEquals(1350, poster?.height)
        assertTrue(session.isCurrentExport(epoch, "public"))
        session.back()
        runCurrent()
        assertFalse(session.isCurrentExport(epoch, "public"))
        assertFalse(session.current().preview)
        assertTrue(events.isEmpty())
    }

    @Test fun cancelledCoverObserverNeverPublishesPrivateData() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repository = object : SavedCollectionsRepository by saved {
            override suspend fun cover(collectionId: String, cover: SavedCollectionCover): Result<ByteArray> {
                withContext(NonCancellable) { gate.await() }
                return Result.success(byteArrayOf(42))
            }
        }
        saved.value = privateList.copy(summary = privateList.summary.copy(cover = SavedCollectionCover("private", true)))
        val session = session(CollectionsWorkspaceMode.Editor, "private", repository)
        runCurrent()
        var delivered = false
        session.loadCover("private", session.current().accountVersion) { _, _ -> delivered = true }
        runCurrent()
        account.owner.value = null
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertFalse(delivered)
    }

    private fun session(mode: CollectionsWorkspaceMode, id: String? = null,
        repository: SavedCollectionsRepository = saved): CollectionsWorkspaceSession =
        CollectionsWorkspaceSession(repository, SavedTestSnapshots(), account, mode, id, null, null,
            { events.add("back") }, { events.add("sign-in") }, { events.add("editor:$it") },
            { events.add("collection:$it") }, {}, {}, {}).also(sessions::add)
}

