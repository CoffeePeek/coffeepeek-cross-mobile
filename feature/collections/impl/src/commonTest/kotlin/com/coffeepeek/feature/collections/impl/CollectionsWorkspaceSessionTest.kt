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
        assertEquals(listOf("saved"), events)
    }

    @Test fun nativeCreationUsesSavedCallbackInsteadOfBack() = runTest(dispatcher) {
        val session = session(CollectionsWorkspaceMode.Editor)
        runCurrent()
        session.title("New collection")
        session.save()
        runCurrent()
        assertEquals(1, saved.creates)
        assertEquals(listOf("saved"), events)
    }

    @Test fun nativeSaveFailureKeepsTheEditorOpen() = runTest(dispatcher) {
        saved.updateFailure = SavedCollectionConflict()
        val session = session(CollectionsWorkspaceMode.Editor, "private")
        runCurrent()
        session.title("My edits")
        session.save()
        runCurrent()
        assertEquals("Conflict", session.current().failure)
        assertEquals("My edits", session.current().title)
        assertTrue(events.isEmpty())
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

    @Test fun nativeSearchProjectsMatchesWithoutRemovingMembershipOrLibraryData() = runTest(dispatcher) {
        val session = session(CollectionsWorkspaceMode.Owner)
        runCurrent()
        val original = session.current().lists
        assertTrue(original.isNotEmpty())
        session.query(original.first().title.uppercase())
        runCurrent()
        assertTrue(session.current().matchingLists.any { it.id == original.first().id })
        assertEquals(original, session.current().lists)
        session.query("no matching collection")
        runCurrent()
        assertTrue(session.current().matchingLists.isEmpty())
        assertEquals(original, session.current().lists)
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

    @Test fun nativePickerCreatesPrivateListAndCommitsItsMembership() = runTest(dispatcher) {
        val session = picker()
        runCurrent()
        assertTrue(session.current().membershipsLoaded)
        session.query("week")
        session.quickTitle("Weekend")
        runCurrent()
        assertTrue(session.current().canCreateQuick)
        session.createQuick()
        runCurrent()
        assertEquals("week", session.current().query)
        assertNull(session.current().quickTitle)
        assertTrue(session.current().lists.single { it.id == "created" }.selected)
        assertTrue(events.isEmpty())
        session.commitMemberships()
        runCurrent()
        assertEquals(setOf("created"), saved.sentMemberships)
        assertEquals(SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "shop"), saved.sentTarget)
        assertEquals(listOf("back"), events)
    }

    @Test fun nativePickerCannotCreateBlankTitle() = runTest(dispatcher) {
        val session = picker()
        runCurrent()
        session.quickTitle("   ")
        session.createQuick()
        runCurrent()
        assertFalse(session.current().canCreateQuick)
        assertEquals(0, saved.creates)
    }

    @Test fun nativeShopPickerCancelsQuickCreationWithoutLosingItsSelection() = runTest(dispatcher) {
        assertQuickCreationCancel(CollectionPlaceKind.CoffeeShop)
    }

    @Test fun nativeRoasterPickerCancelsQuickCreationWithoutLosingItsSelection() = runTest(dispatcher) {
        assertQuickCreationCancel(CollectionPlaceKind.Roaster)
    }

    private fun TestScope.assertQuickCreationCancel(kind: CollectionPlaceKind) {
        val session = draftPicker(kind)
        assertTrue(session.current().canCreateQuick)
        session.quickTitle(null)
        runCurrent()
        assertPickerSelectionRetained(session)
        assertEquals(0, saved.creates)
        assertEquals(0, saved.membershipWrites)
        assertTrue(events.isEmpty())
        session.back()
        runCurrent()
        assertEquals(listOf("back"), events)
        assertEquals(0, saved.membershipWrites)
    }

    @Test fun nativePickerCanCancelAfterQuickCreationFails() = runTest(dispatcher) {
        saved.createGate = CompletableDeferred()
        saved.createFailure = IllegalStateException("Offline")
        val session = draftPicker(CollectionPlaceKind.CoffeeShop)
        session.createQuick()
        runCurrent()
        assertTrue(session.current().busy)
        saved.createGate?.complete(Unit)
        runCurrent()
        assertFalse(session.current().busy)
        assertEquals("Save", session.current().failure)
        session.quickTitle(null)
        runCurrent()
        assertPickerSelectionRetained(session)
        assertEquals(0, saved.membershipWrites)
    }

    @Test fun nativePickerBackClosesAnActiveDraftWithoutWriting() = runTest(dispatcher) {
        val session = draftPicker(CollectionPlaceKind.Roaster)
        session.back()
        runCurrent()
        assertEquals(listOf("back"), events)
        session.close()
        session.createQuick()
        session.commitMemberships()
        runCurrent()
        assertEquals(0, saved.creates)
        assertEquals(0, saved.membershipWrites)
    }

    private fun TestScope.draftPicker(kind: CollectionPlaceKind): CollectionsWorkspaceSession {
        val session = picker(kind)
        runCurrent()
        session.query("Public")
        session.toggleMembership("public")
        session.quickTitle("Unfinished list")
        runCurrent()
        return session
    }

    private fun assertPickerSelectionRetained(session: CollectionsWorkspaceSession) {
        assertNull(session.current().quickTitle)
        assertEquals("Public", session.current().query)
        assertTrue(session.current().lists.single { it.id == "public" }.selected)
    }

    @Test fun nativePickerKeepsSelectionsAfterFailureAndRejectsWritesAfterClose() = runTest(dispatcher) {
        val session = picker()
        runCurrent()
        session.toggleMembership("public")
        saved.membershipFailure = IllegalStateException("Offline")
        session.commitMemberships()
        runCurrent()
        assertEquals("Membership", session.current().failure)
        assertTrue(session.current().lists.single { it.id == "public" }.selected)
        assertTrue(events.isEmpty())
        session.close()
        session.quickTitle("Too late")
        session.createQuick()
        session.commitMemberships()
        runCurrent()
        assertEquals(0, saved.creates)
        assertEquals(1, saved.membershipWrites)
    }

    @Test fun nativePickerClearsQuickCreationAndSelectionOnAccountChange() = runTest(dispatcher) {
        val session = picker()
        runCurrent()
        session.quickTitle("Private draft")
        session.toggleMembership("public")
        runCurrent()
        account.owner.value = null
        runCurrent()
        assertNull(session.current().quickTitle)
        assertTrue(session.current().lists.isEmpty())
        assertFalse(session.current().canCreateQuick)
        session.createQuick()
        session.commitMemberships()
        runCurrent()
        assertEquals(0, saved.creates)
        assertEquals(0, saved.membershipWrites)
    }

    private fun picker(kind: CollectionPlaceKind = CollectionPlaceKind.CoffeeShop): CollectionsWorkspaceSession = CollectionsWorkspaceSession(saved, SavedTestSnapshots(),
        account, CollectionsWorkspaceMode.Picker, null, null,
        SavedCollectionTarget(kind, if (kind == CollectionPlaceKind.CoffeeShop) "shop" else "roaster"),
        { events.add("back") }, { events.add("sign-in") }, {}, {}, {}, {}, {}, onSaved = { events.add("saved") }).also(sessions::add)

    private fun session(mode: CollectionsWorkspaceMode, id: String? = null,
        repository: SavedCollectionsRepository = saved): CollectionsWorkspaceSession =
        CollectionsWorkspaceSession(repository, SavedTestSnapshots(), account, mode, id, null, null,
            { events.add("back") }, { events.add("sign-in") }, { events.add("editor:$it") },
            { events.add("collection:$it") }, {}, {}, {}, onSaved = { events.add("saved") }).also(sessions::add)
}

