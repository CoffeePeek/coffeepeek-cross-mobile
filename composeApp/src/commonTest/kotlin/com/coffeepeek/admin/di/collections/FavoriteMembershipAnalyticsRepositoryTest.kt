package com.coffeepeek.admin.di.collections

import com.coffeepeek.domain.feature.favorites.api.FavoriteEntry
import com.coffeepeek.domain.feature.favorites.api.FavoriteKind
import com.coffeepeek.domain.model.PublicAddress
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.model.SavedCollectionSummary
import com.coffeepeek.feature.collections.domain.model.SavedCollectionTarget
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteMembershipAnalyticsRepositoryTest {
    @Test fun targetMatchUsesKindAndCanonicalSlug() {
        val shop = FavoriteEntry(FavoriteKind.CoffeeShop, PublicAddress("same", "/shops/same", 1, false))
        val roaster = FavoriteEntry(FavoriteKind.Roaster, PublicAddress("same", "/roasters/same", 1, false))
        assertTrue(shop.matches(SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "same")))
        assertFalse(roaster.matches(SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "same")))
        assertFalse(shop.matches(SavedCollectionTarget(CollectionPlaceKind.CoffeeShop, "other")))
    }

    @Test fun movingBetweenListsKeepsTheEffectiveHeartAndClearingAllRemovesIt() {
        val lists = listOf(SavedCollectionSummary("one", "One", containsItem = true), SavedCollectionSummary("two", "Two"))
        assertTrue(selectedAfterMembershipChange(lists, setOf("two"), setOf("one")))
        assertFalse(selectedAfterMembershipChange(lists, emptySet(), setOf("one")))
        assertFalse(selectedAfterMembershipChange(emptyList(), emptySet(), emptySet()))
        assertTrue(selectedAfterMembershipChange(emptyList(), setOf("one"), emptySet()))
    }
}
