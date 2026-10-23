package com.coffeepeek.admin.ui.favorites

import com.coffeepeek.admin.feature.favorites.ui.shouldShowProfilePreview
import com.coffeepeek.admin.ui.screen.favorites.FavoritesUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritesProfilePreviewVisibilityTest {
    private val empty = FavoritesUiState(areRoastersLoading = false)

    @Test fun successfullyLoadedEmptyFavoritesStayHidden() {
        assertFalse(empty.shouldShowProfilePreview())
    }

    @Test fun emptyRoasterLoadFailureRemainsVisibleUntilDismissed() {
        val failed = empty.copy(actionMessage = "load failed")
        assertTrue(failed.shouldShowProfilePreview())
        assertFalse(failed.copy(actionMessage = null).shouldShowProfilePreview())
    }

    @Test fun loadingAndErrorsKeepAnEmptyPreviewVisible() {
        assertTrue(empty.copy(isLoading = true).shouldShowProfilePreview())
        assertTrue(empty.copy(areRoastersLoading = true).shouldShowProfilePreview())
        assertTrue(empty.copy(error = "load failed").shouldShowProfilePreview())
        assertTrue(empty.copy(roasterError = "load failed").shouldShowProfilePreview())
    }
}
