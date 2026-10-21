package com.coffeepeek.admin.di.contributions

import com.coffeepeek.admin.feature.community.ui.FeedSessions
import com.coffeepeek.core.session.Session
import com.coffeepeek.domain.model.*
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.contributions.domain.model.Availability
import com.coffeepeek.feature.contributions.domain.model.ShopPhotoRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ContributionsAdaptersTest {
    @Test fun theAccountKeyIsTheUserIdElseTheTokenAndNullForAGuestOrAnInactiveSession() = runTest {
        val sessions = FeedSessions(null)
        val account = SessionContributionsAccount(sessions)
        assertNull(account.accountKey.first()); assertNull(account.currentAccountKey())
        sessions.applySession(Session("token", userId = "user-1"))
        assertEquals("user-1", account.accountKey.first()); assertEquals("user-1", account.currentAccountKey())
        sessions.applySession(Session("token-only"))
        assertEquals("token-only", account.currentAccountKey())
        sessions.applySession(Session(" ", userId = "user-1"))
        assertNull(account.currentAccountKey()); assertNull(account.accountKey.first())
    }

    @Test fun theCatalogsKeepTheRoasterLogoAndMapEveryList() = runTest {
        val shops = Shops(catalogs = Result.success(ShopCatalogs(
            cities = listOf(City("minsk", "Минск")),
            beans = listOf(CatalogItem("b", "Арабика")),
            equipment = listOf(CatalogItem("e", "La Marzocco")),
            roasters = listOf(CatalogItem("r", "Roaster", photoUrl = "https://logo")),
            brewMethods = listOf(CatalogItem("m", "V60")),
            shopTags = listOf(CatalogItem("t", "Wi-Fi")),
        )))
        val all = ShopContributionCatalogs(shops).catalogs().getOrThrow()
        assertEquals(listOf("minsk"), all.cities.map { it.id }); assertEquals("https://logo", all.roasters.single().photoUrl)
        assertEquals(listOf("b"), all.beans.map { it.id }); assertEquals(listOf("e"), all.equipment.map { it.id })
        assertEquals(listOf("m"), all.brewMethods.map { it.id }); assertEquals(listOf("t"), all.tags.map { it.id })
    }

    @Test fun theShopSnapshotCarriesTheEditablePartsOnly() = runTest {
        val shops = Shops(details = Result.success(CoffeeShopDetails(
            shop = CoffeeShop("id", "Cafe", rating = null, cityName = null, priceRange = null, photoUrl = null),
            description = "Text",
            shopPhotos = listOf(ShopPhoto("p1", "https://full", "https://preview")),
            contact = ShopContact(instagram = "https://instagram.com/cafe", phone = "+375"),
            tagItems = listOf(CatalogItem("t", "Wi-Fi")), roasters = listOf(CatalogItem("r", "R")),
            equipmentItems = listOf(CatalogItem("e", "E")), brewMethodItems = listOf(CatalogItem("m", "M")),
            menu = ShopMenu(
                items = listOf(
                    ShopMenuItem("espresso", "Эспрессо", category = "coffee", availability = "Present", price = 4.5),
                    ShopMenuItem("latte", "Латте", category = "coffee", availability = "2"),
                    ShopMenuItem("flat", "Флэт", category = "coffee", availability = "unknown"),
                ),
                photos = listOf(ShopMenuPhoto("m1", "https://menu-full", "https://menu-preview")),
            ),
        )))
        val snapshot = ShopContributionCatalogs(shops).shop("cafe-slug").getOrThrow()
        assertEquals("cafe-slug", snapshot.slug); assertEquals("Cafe", snapshot.title); assertEquals("Text", snapshot.description)
        assertEquals("+375", snapshot.contacts.phone); assertEquals("https://instagram.com/cafe", snapshot.contacts.instagram)
        assertEquals(listOf(ShopPhotoRef("p1", "https://preview")), snapshot.photos)
        assertEquals(listOf("t"), snapshot.tagIds); assertEquals(listOf("r"), snapshot.roasterIds)
        assertEquals(listOf("e"), snapshot.equipmentIds); assertEquals(listOf("m"), snapshot.brewMethodIds)
        assertEquals(listOf(Availability.Present, Availability.Absent, Availability.Unknown), snapshot.menu.map { it.availability })
        assertEquals(4.5, snapshot.menu.first().price)
        assertEquals(listOf(ShopPhotoRef("m1", "https://menu-preview")), snapshot.menuPhotos)
    }

    @Test fun drinksUseTheRussianNameThenTheEnglishOneThenTheSlug() = runTest {
        val shops = Shops(drinks = Result.success(listOf(
            CoffeeDrinkDefinition("a", "Эспрессо", "Espresso", "c", 1),
            CoffeeDrinkDefinition("b", " ", "Latte", "c", 2),
            CoffeeDrinkDefinition("c", "", "", "c", 3),
        )))
        assertEquals(listOf("Эспрессо", "Latte", "c"), ShopContributionCatalogs(shops).drinks().getOrThrow().map { it.name })
    }

    @Test fun aFailureIsPassedOn() = runTest {
        val shops = Shops(catalogs = Result.failure(IllegalStateException("offline")), details = Result.failure(IllegalStateException("gone")))
        assertTrue(ShopContributionCatalogs(shops).catalogs().isFailure); assertTrue(ShopContributionCatalogs(shops).shop("x").isFailure)
    }

    private class Shops(
        private val catalogs: Result<ShopCatalogs> = Result.success(ShopCatalogs()),
        private val details: Result<CoffeeShopDetails> = Result.failure(IllegalStateException("unused")),
        private val drinks: Result<List<CoffeeDrinkDefinition>> = Result.success(emptyList()),
    ) : ShopRepository {
        override suspend fun searchShops(filters: ShopFilters): Result<PagedResult<CoffeeShop>> = error("unused")
        override suspend fun getShopDetails(id: String) = details
        override suspend fun getMapContent(bounds: MapBounds, zoom: Float, filters: ShopFilters): Result<MapContent> = error("unused")
        override suspend fun getCatalogs() = catalogs
        override suspend fun getConsumedDrinks(): Result<List<ConsumedDrinkOption>> = error("unused")
        override suspend fun getMenuDrinks() = drinks
        override suspend fun createShop(input: CreateShopInput): Result<Unit> = error("unused")
        override suspend fun getMyShopSubmissions(status: ModerationStatus, page: Int, pageSize: Int): Result<PagedResult<ShopSubmission>> = error("unused")
    }
}
