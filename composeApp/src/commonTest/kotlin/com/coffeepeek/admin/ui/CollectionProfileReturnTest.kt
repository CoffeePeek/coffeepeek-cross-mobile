package com.coffeepeek.admin.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionProfileReturnTest {
    private val dispatcher = StandardTestDispatcher()
    private val owner = RootNavigationSession(true, "owner", 1)
    private val origin = RootNavigationOrigin(owner)
    private val saved = Navigator.NavEvent.ReturnToProfile(origin)

    @BeforeTest fun before() {
        Dispatchers.setMain(dispatcher)
        clearRequests()
    }

    @AfterTest fun after() {
        clearRequests()
        Dispatchers.resetMain()
    }

    @Test fun acceptedSaveSelectsCollectionsInsteadOfAnEarlierShelfRequest() = runTest(dispatcher) {
        Navigator.returnToProfileShelf()
        runCurrent()
        assertTrue(Navigator.pendingProfileShelf.value)
        assertEquals(Navigator.Screen.ProfileTab, Navigator.pendingTabSelection.value)
        assertTrue(Navigator.prepareRootEvent(saved, origin, owner))
        assertTrue(Navigator.pendingProfileCollections.value)
        assertFalse(Navigator.pendingProfileShelf.value)
        assertFalse(Navigator.pendingProfileCheckIns.value)
        assertNull(Navigator.pendingTabSelection.value)
    }

    @Test fun acceptedSaveSupersedesHistoryUntilTheProfileConsumesIt() = runTest(dispatcher) {
        Navigator.openProfileCheckIns()
        runCurrent()
        Navigator.prepareRootEvent(saved, origin, owner)
        runCurrent()
        assertTrue(Navigator.pendingProfileCollections.value)
        assertFalse(Navigator.pendingProfileCheckIns.value)
        Navigator.consumeProfileCollections()
        assertFalse(Navigator.pendingProfileCollections.value)
        Navigator.consumeProfileCollections()
        assertFalse(Navigator.pendingProfileCollections.value)
        Navigator.prepareRootEvent(saved, origin, owner)
        assertTrue(Navigator.pendingProfileCollections.value)
    }

    @Test fun aNewShelfIntentSupersedesTheSavedCollectionSection() = runTest(dispatcher) {
        Navigator.prepareRootEvent(saved, origin, owner)
        Navigator.returnToProfileShelf()
        runCurrent()
        assertFalse(Navigator.pendingProfileCollections.value)
        assertTrue(Navigator.pendingProfileShelf.value)
        assertFalse(Navigator.pendingProfileCheckIns.value)
    }

    @Test fun aNewHistoryIntentSupersedesTheSavedCollectionSection() = runTest(dispatcher) {
        Navigator.prepareRootEvent(saved, origin, owner)
        Navigator.openProfileCheckIns()
        runCurrent()
        assertFalse(Navigator.pendingProfileCollections.value)
        assertFalse(Navigator.pendingProfileShelf.value)
        assertTrue(Navigator.pendingProfileCheckIns.value)
    }

    @Test fun ordinaryProfileSelectionDoesNotReplayAConsumedCollectionIntent() = runTest(dispatcher) {
        Navigator.prepareRootEvent(saved, origin, owner)
        Navigator.consumeProfileCollections()
        Navigator.selectTab(Navigator.Screen.ProfileTab)
        runCurrent()
        assertFalse(Navigator.pendingProfileCollections.value)
        assertFalse(Navigator.pendingProfileShelf.value)
        assertFalse(Navigator.pendingProfileCheckIns.value)
    }

    @Test fun obsoleteAccountCompletionCannotConsumeTheSuccessorsSectionRequest() = runTest(dispatcher) {
        Navigator.returnToProfileShelf()
        runCurrent()
        val successor = RootNavigationSession(true, "successor", 2)
        assertFalse(Navigator.prepareRootEvent(saved, RootNavigationOrigin(successor), successor))
        assertFalse(Navigator.pendingProfileCollections.value)
        assertTrue(Navigator.pendingProfileShelf.value)
        assertEquals(Navigator.Screen.ProfileTab, Navigator.pendingTabSelection.value)
    }

    @Test fun conflatedSameAccountReplacementCannotRequestCollections() {
        assertFalse(Navigator.prepareRootEvent(saved, origin, owner.copy(generation = 3)))
        assertFalse(Navigator.pendingProfileCollections.value)
    }

    @Test fun aRecreatedRootRejectsTheOldEditorsPendingCompletion() {
        assertFalse(Navigator.prepareRootEvent(saved, RootNavigationOrigin(owner), owner))
        assertFalse(Navigator.pendingProfileCollections.value)
    }

    @Test fun accountReplacementAndLogoutClearUnconsumedCollectionRequests() {
        val replacement = owner.copy(accountId = "successor", generation = 2)
        Navigator.prepareRootEvent(saved, origin, owner)
        Navigator.prepareRootTransition(RootSessionTransition(owner, replacement))
        assertFalse(Navigator.pendingProfileCollections.value)
        Navigator.prepareRootEvent(saved, origin, owner)
        Navigator.prepareRootTransition(RootSessionTransition(owner, RootNavigationSession(false, null, 3)))
        assertFalse(Navigator.pendingProfileCollections.value)
    }

    @Test fun cancellingAnEditorRetainsTheLibraryAndDoesNotRequestAProfileSection() {
        val model = NativeRootNavigationModel(true)
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.CollectionsLibrary)))
        model.confirm(model.plan(Navigator.NavEvent.NavigateTo(Navigator.Screen.CollectionEditor("edited"))))
        assertTrue(Navigator.prepareRootEvent(Navigator.NavEvent.PopBack, origin, owner))
        model.confirm(model.plan(Navigator.NavEvent.PopBack))
        assertEquals(Navigator.Screen.CollectionsLibrary, model.actual.stacks[0].last().screen)
        assertFalse(Navigator.pendingProfileCollections.value)
    }

    private fun clearRequests() {
        Navigator.consumeProfileCollections()
        Navigator.consumeProfileShelf()
        Navigator.consumeProfileCheckIns()
        Navigator.consumeTabSelection()
    }
}
