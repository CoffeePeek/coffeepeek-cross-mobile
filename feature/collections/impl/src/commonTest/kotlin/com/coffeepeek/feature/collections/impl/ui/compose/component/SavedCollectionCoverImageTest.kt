package com.coffeepeek.feature.collections.impl.ui.compose.component

import com.coffeepeek.feature.collections.domain.model.CollectionPlace
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.impl.ui.SavedTestRepository
import com.coffeepeek.feature.collections.impl.ui.privateList
import com.coffeepeek.feature.collections.impl.ui.publicList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedCollectionCoverImageTest {
    @Test fun privateCoverUsesFirstShopPhotoAfterRoasters() = runTest {
        val saved = SavedTestRepository()
        saved.value = privateList.copy(places = listOf(
            CollectionPlace(CollectionPlaceKind.Roaster, "roaster", "Roaster", "https://fixture.invalid/roaster.jpg"),
            CollectionPlace(CollectionPlaceKind.CoffeeShop, "first", "First", "https://fixture.invalid/first.jpg"),
            CollectionPlace(CollectionPlaceKind.CoffeeShop, "second", "Second", "https://fixture.invalid/second.jpg"),
        ))

        assertEquals("https://fixture.invalid/first.jpg", defaultShopPhotoUrl(saved.value.summary, saved))
        assertEquals(0, saved.publicReads)
    }

    @Test fun publicCoverReadsPublicCollectionWithoutOwnerAccess() = runTest {
        val saved = SavedTestRepository()

        assertNull(defaultShopPhotoUrl(publicList.summary, saved))
        assertEquals(1, saved.publicReads)
    }

    @Test fun firstShopWithoutPhotoKeepsPlaceholder() = runTest {
        val saved = SavedTestRepository()
        saved.value = privateList.copy(places = listOf(
            CollectionPlace(CollectionPlaceKind.CoffeeShop, "first", "First"),
            CollectionPlace(CollectionPlaceKind.CoffeeShop, "second", "Second", "https://fixture.invalid/second.jpg"),
        ))

        assertNull(defaultShopPhotoUrl(saved.value.summary, saved))
    }
}
