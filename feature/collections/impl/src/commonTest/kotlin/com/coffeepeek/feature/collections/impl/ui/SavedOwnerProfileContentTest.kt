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

    @Test fun refreshHidesPreviouslyLoadedCollections() {
        assertEquals(SavedOwnerProfileContent.Status, savedOwnerProfileContent(ready.copy(loading = true)))
    }

    @Test fun failedReadDoesNotExposeStaleCollections() {
        assertEquals(SavedOwnerProfileContent.Status,
            savedOwnerProfileContent(ready.copy(failure = SavedCollectionsFailure.Load)))
    }

    @Test fun signedOutStateCannotRenderThePreviousOwnersCollections() {
        assertEquals(SavedOwnerProfileContent.Status, savedOwnerProfileContent(ready.copy(signedIn = false)))
    }
}
