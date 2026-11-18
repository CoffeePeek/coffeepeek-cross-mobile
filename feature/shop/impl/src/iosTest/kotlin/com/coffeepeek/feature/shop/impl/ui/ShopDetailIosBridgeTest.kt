package com.coffeepeek.feature.shop.impl.ui

import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.feature.shop.impl.ui.compose.model.*
import kotlin.test.*

class ShopDetailIosBridgeTest {
    @Test fun guestSnapshotNeverExportsHiddenPreviewIdentitiesOrImages() {
        val snapshot = detailCheckInSnapshot(state(), "ru")
        assertEquals(listOf("visit-0"), snapshot.rows.map { it.id })
        assertEquals(2, snapshot.hiddenCount)
        assertFalse(snapshot.rows.single().canBlock)
        assertTrue(snapshot.showGuestPrompt)
    }

    @Test fun signedInPreviewKeepsTheThreeCardLimitAndExistingOwnAuthorRules() {
        val snapshot = detailCheckInSnapshot(state().copy(isLoggedIn = true, currentUserId = "author-0"), "be")
        assertEquals(3, snapshot.rows.size)
        assertEquals(0, snapshot.hiddenCount)
        assertTrue(snapshot.rows.first().isOwn)
        assertFalse(snapshot.rows.first().canReport)
        assertFalse(snapshot.rows.first().canBlock)
    }

    @Test fun nativePhotoIntentsUseCurrentPreviewAndOriginalSourcesWithValidatedIndices() {
        val hero = shopDetailIosIntent("hero-photo", "ignored", 0, state()) as ShopDetailAction.OpenPhoto
        assertEquals(listOf("hero-preview"), hero.urls)
        assertEquals(listOf("hero-original"), hero.originals)
        val menu = shopDetailIosIntent("menu-photo", "ignored", 0, state()) as ShopDetailAction.OpenPhoto
        assertEquals(listOf("menu-preview"), menu.urls)
        assertEquals(listOf("menu-original"), menu.originals)
        assertNull(shopDetailIosIntent("hero-photo", "", -1, state()))
        assertNull(shopDetailIosIntent("menu-photo", "", 1, state()))
    }

    @Test fun guestCannotInvokeActionsForHiddenOrReplacedCheckInRows() {
        for (name in listOf("vote", "author", "report", "block", "checkin-photo")) {
            assertNull(shopDetailIosIntent(name, "visit-1", 0, state()))
            assertNull(shopDetailIosIntent(name, "obsolete", 0, state().copy(isLoggedIn = true)))
        }
        assertEquals(ShopDetailAction.OpenCheckInPhoto("visit-0", 0),
            shopDetailIosIntent("checkin-photo", "visit-0", 0, state()))
    }

    @Test fun nativeContactsAcceptOnlyTheExistingValidatedFeatureTargets() {
        assertEquals(ShopDetailAction.OpenLink("https://coffeepeek.app"),
            shopDetailIosIntent("link", "https://coffeepeek.app", 0, state()))
        assertNull(shopDetailIosIntent("link", "javascript:alert(1)", 0, state()))
        assertNull(shopDetailIosIntent("copy", "another-number", 0, state()))
        assertEquals(ShopDetailAction.CopyPhone("+375 29 123 45 67"),
            shopDetailIosIntent("copy", "+375 29 123 45 67", 0, state()))
    }

    @Test fun disposingTheNativeSurfaceClearsEveryProtectedRenderingRowAndDate() {
        val source = nativeState()
        assertTrue(source.checkIns.rows.isNotEmpty())
        val cleared = source.withoutContent("ru")
        assertTrue(cleared.checkIns.rows.isEmpty())
        assertTrue(cleared.checkInDates.isEmpty())
        assertNull(cleared.message)
        assertEquals(source.generation, cleared.generation)
    }

    private fun nativeState() = ShopDetailIosState(
        generation = 7,
        loading = false,
        hasError = false,
        loggedIn = true,
        favorite = false,
        favoriteEnabled = true,
        favoriteBusy = false,
        favoriteSelection = false,
        favoriteLabel = "favorite",
        scheduleExpanded = false,
        featuresExpanded = false,
        priceInfoVisible = false,
        message = "message",
        details = null,
        checkIns = detailCheckInSnapshot(state().copy(isLoggedIn = true), "ru"),
        labels = emptyMap(),
        checkInDates = mapOf("visit-0" to "date"),
    )

    private fun state(): ShopDetailState = ShopDetailState(isLoading = false, details = ShopDetails(
        overview = ShopOverview("shop", "Shop", null, "Address", 53.9, 27.5, 4.5, 4, true,
            listOf(ShopPhoto("hero", "hero-preview", "hero-original"))),
        menu = ShopMenu(photos = listOf(MenuPhoto("menu", "menu-original", "menu-preview"))),
        schedules = emptyList(), coffee = ShopCoffeeDetails(), features = emptyList(), reviews = emptyList(),
        contact = ShopContact(phone = "+375 29 123 45 67", website = "coffeepeek.app"),
        userCheckIns = emptyList(), checkIns = List(4) { index ->
            ShopCheckIn("visit-$index", "author-$index", "shop", "note", "", "", null,
                listOf("photo-$index"), listOf("thumbnail-$index"), null,
                visibility = ShopCheckInVisibility.Public, authorSlug = "author-$index")
        }))
}
