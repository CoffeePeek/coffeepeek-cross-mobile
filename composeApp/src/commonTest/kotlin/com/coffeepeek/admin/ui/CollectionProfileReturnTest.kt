package com.coffeepeek.admin.ui

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CollectionProfileReturnTest {
    private val owner = RootNavigationSession(true, "owner", 1)
    private val origin = RootNavigationOrigin(owner)
    private val saved = Navigator.NavEvent.ReturnToProfile(origin)

    @BeforeTest fun before() {
        clearRequests()
    }

    @AfterTest fun after() {
        clearRequests()
    }

    @Test fun acceptedSaveSelectsCollectionsInsteadOfAnEarlierShelfRequest() = runTest {
        selectProfile { Navigator.returnToProfileShelf() }
        assertTrue(Navigator.pendingProfileShelf.value)
        assertEquals(Navigator.Screen.ProfileTab, Navigator.pendingTabSelection.value)
        assertTrue(Navigator.prepareRootEvent(saved, origin, owner))
        assertTrue(Navigator.pendingProfileCollections.value)
        assertFalse(Navigator.pendingProfileShelf.value)
        assertFalse(Navigator.pendingProfileCheckIns.value)
        assertNull(Navigator.pendingTabSelection.value)
    }

    @Test fun acceptedSaveSupersedesHistoryUntilTheProfileConsumesIt() = runTest {
        selectProfile { Navigator.openProfileCheckIns() }
        Navigator.prepareRootEvent(saved, origin, owner)
        assertTrue(Navigator.pendingProfileCollections.value)
        assertFalse(Navigator.pendingProfileCheckIns.value)
        Navigator.consumeProfileCollections()
        assertFalse(Navigator.pendingProfileCollections.value)
        Navigator.consumeProfileCollections()
        assertFalse(Navigator.pendingProfileCollections.value)
        Navigator.prepareRootEvent(saved, origin, owner)
        assertTrue(Navigator.pendingProfileCollections.value)
    }

    @Test fun aNewShelfIntentSupersedesTheSavedCollectionSection() = runTest {
        Navigator.prepareRootEvent(saved, origin, owner)
        selectProfile { Navigator.returnToProfileShelf() }
        assertFalse(Navigator.pendingProfileCollections.value)
        assertTrue(Navigator.pendingProfileShelf.value)
        assertFalse(Navigator.pendingProfileCheckIns.value)
    }

    @Test fun aNewHistoryIntentSupersedesTheSavedCollectionSection() = runTest {
        Navigator.prepareRootEvent(saved, origin, owner)
        selectProfile { Navigator.openProfileCheckIns() }
        assertFalse(Navigator.pendingProfileCollections.value)
        assertFalse(Navigator.pendingProfileShelf.value)
        assertTrue(Navigator.pendingProfileCheckIns.value)
    }

    @Test fun ordinaryProfileSelectionDoesNotReplayAConsumedCollectionIntent() = runTest {
        Navigator.prepareRootEvent(saved, origin, owner)
        Navigator.consumeProfileCollections()
        selectProfile { Navigator.selectTab(Navigator.Screen.ProfileTab) }
        assertFalse(Navigator.pendingProfileCollections.value)
        assertFalse(Navigator.pendingProfileShelf.value)
        assertFalse(Navigator.pendingProfileCheckIns.value)
    }

    @Test fun obsoleteAccountCompletionCannotConsumeTheSuccessorsSectionRequest() = runTest {
        selectProfile { Navigator.returnToProfileShelf() }
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

    private suspend fun TestScope.selectProfile(action: () -> Unit) {
        val selected = async(start = CoroutineStart.UNDISPATCHED) {
            Navigator.navigationEvents.first { it == Navigator.NavEvent.SelectTab(Navigator.Screen.ProfileTab) }
        }
        action()
        selected.await()
    }

    private fun clearRequests() {
        Navigator.consumeProfileCollections()
        Navigator.consumeProfileShelf()
        Navigator.consumeProfileCheckIns()
        Navigator.consumeTabSelection()
    }
}
