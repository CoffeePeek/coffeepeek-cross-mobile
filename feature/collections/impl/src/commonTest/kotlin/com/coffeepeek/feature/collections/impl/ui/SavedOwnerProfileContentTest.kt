package com.coffeepeek.feature.collections.impl.ui

import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlin.test.*

class SavedOwnerProfileContentTest {
    private val ready = SavedCollectionsState(SavedCollectionsMode.Library, signedIn = true, loading = false,
        lists = listOf(privateList.summary, publicList.summary))

    @Test fun ownerPreviewKeepsPrivateAndPublicListsInLibraryOrder() {
        val content = assertIs<SavedOwnerProfileContent.Lists>(savedOwnerProfileContent(ready))
        assertEquals(ready.lists, content.values)
    }

    @Test fun successfulEmptyReadShowsEmptyState() {
        assertEquals(SavedOwnerProfileContent.Empty, savedOwnerProfileContent(ready.copy(lists = emptyList())))
    }

    @Test fun backgroundRefreshKeepsPreviouslyLoadedCollections() {
        val content = assertIs<SavedOwnerProfileContent.Lists>(savedOwnerProfileContent(ready.copy(refreshing = true)))
        assertEquals(ready.lists, content.values)
    }

    @Test fun failedRefreshKeepsTheVerifiedOwnerSnapshot() {
        val content = assertIs<SavedOwnerProfileContent.Lists>(savedOwnerProfileContent(ready.copy(failure = SavedCollectionsFailure.Load)))
        assertEquals(ready.lists, content.values)
    }

    @Test fun initialLoadWithoutVerifiedSnapshotShowsStatus() {
        assertEquals(SavedOwnerProfileContent.Status, savedOwnerProfileContent(ready.copy(listsLoaded = false)))
    }

    @Test fun signedOutStateCannotRenderThePreviousOwnersCollections() {
        assertEquals(SavedOwnerProfileContent.Status, savedOwnerProfileContent(ready.copy(signedIn = false)))
    }
}
