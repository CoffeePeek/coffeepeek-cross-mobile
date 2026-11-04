package com.coffeepeek.feature.collections.impl.ui

import com.coffeepeek.feature.collections.domain.model.SavedCollectionVisibility
import com.coffeepeek.feature.collections.impl.ui.model.*
import kotlin.test.*

class SavedOwnerProfileContentTest {
    private val ready = SavedCollectionsState(SavedCollectionsMode.Preference, signedIn = true, loading = false,
        lists = listOf(publicList.summary), featuredId = publicList.summary.id)

    @Test fun selectedPublicCollectionKeepsItsActualTitle() {
        val content = assertIs<SavedOwnerProfileContent.Selected>(savedOwnerProfileContent(ready))
        assertEquals(publicList.summary.title, content.value.title)
        assertEquals(publicList.summary.id, content.value.id)
    }

    @Test fun successfulOptOutUsesTheExistingFavoritesSection() {
        assertEquals(SavedOwnerProfileContent.Favorites, savedOwnerProfileContent(ready.copy(featuredId = null)))
    }

    @Test fun refreshHidesPreviouslySelectedContentUntilItsPreferenceIsKnown() {
        assertEquals(SavedOwnerProfileContent.Status, savedOwnerProfileContent(ready.copy(loading = true)))
    }

    @Test fun failedPreferenceReadDoesNotExposeFallbackFavorites() {
        assertEquals(SavedOwnerProfileContent.Status,
            savedOwnerProfileContent(ready.copy(featuredId = null, failure = SavedCollectionsFailure.Load)))
    }

    @Test fun missingOrPrivateSelectionNeverBecomesAGlobalFavoritesFallback() {
        assertEquals(SavedOwnerProfileContent.Status, savedOwnerProfileContent(ready.copy(lists = emptyList())))
        assertEquals(SavedOwnerProfileContent.Status, savedOwnerProfileContent(ready.copy(
            lists = listOf(publicList.summary.copy(visibility = SavedCollectionVisibility.Private)))))
    }

    @Test fun signedOutStateCannotRenderThePreviousOwnersSelection() {
        assertEquals(SavedOwnerProfileContent.Status, savedOwnerProfileContent(ready.copy(signedIn = false)))
    }
}
