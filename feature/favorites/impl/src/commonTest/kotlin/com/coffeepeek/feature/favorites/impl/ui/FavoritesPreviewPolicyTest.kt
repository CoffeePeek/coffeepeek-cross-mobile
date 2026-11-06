package com.coffeepeek.feature.favorites.impl.ui

import com.coffeepeek.feature.favorites.domain.model.*
import com.coffeepeek.feature.favorites.impl.ui.compose.shouldShowPreview
import com.coffeepeek.feature.favorites.impl.ui.compose.model.FavoritesUiState
import kotlin.test.*

/** Pure presentation policy, without Compose instrumentation. */
class FavoritesPreviewPolicyTest {
    private val empty = FavoritesUiState(isLoading = false)

    @Test fun settledEmptyPreviewIsHidden() {
        assertFalse(empty.shouldShowPreview())
    }

    @Test fun everyLoadingAndErrorSourceKeepsItsPreviewVisible() {
        val states = listOf(empty.copy(isLoading = true), empty.copy(areRoastersLoading = true),
            empty.copy(areDrinksLoading = true), empty.copy(loadFailed = true),
            empty.copy(roastersLoadFailed = true), empty.copy(drinksLoadFailed = true),
            empty.copy(actionFailed = true))
        assertTrue(states.all { it.shouldShowPreview() })
    }

    @Test fun eachSavedKindCanDisplayThePreviewOnItsOwn() {
        val states = listOf(
            empty.copy(shops = listOf(FavoriteShop("shop", "Shop"))),
            empty.copy(roasters = listOf(FavoriteRoaster("roast", "Roaster"))),
            empty.copy(drinks = listOf(FavoriteDrink("latte", "Latte", null, true))),
        )
        assertTrue(states.all { it.shouldShowPreview() })
    }
}
