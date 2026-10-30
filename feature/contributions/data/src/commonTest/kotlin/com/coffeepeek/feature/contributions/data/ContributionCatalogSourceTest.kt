package com.coffeepeek.feature.contributions.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.contributions.data.repository.createContributionCatalogSource
import com.coffeepeek.feature.contributions.domain.model.Availability
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.ShopPhotoRef
import com.coffeepeek.feature.contributions.domain.repository.ContributionCatalogSource
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContributionCatalogSourceTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val replies = mutableMapOf<String, () -> Pair<HttpStatusCode, String>>()
    private var roasters: Result<List<CatalogOption>> = Result.success(listOf(CatalogOption("r1", "Roaster", "https://logo")))

    @Test fun theSnapshotKeepsThePublishedMenuRevisionWithoutInventingOne() = runBlocking {
        defaults()
        for (revision in listOf<Long?>(null, 0L, 17L, -1L)) {
            val field = revision?.let { ",\"inputRevision\":$it" }.orEmpty()
            replies["/api/CoffeeShops/cafe"] = { ok("""{"name":"Cafe","menu":{"items":[]$field}}""") }
            assertEquals(revision, source().shop("cafe").getOrThrow().menuInputRevision)
        }
    }

    @Test fun aShopWithoutAMenuHasRevisionZero() = runBlocking {
        defaults()
        replies["/api/CoffeeShops/cafe"] = { ok("""{"name":"Cafe"}""") }
        assertEquals(0L, source().shop("cafe").getOrThrow().menuInputRevision)
    }

    private fun ok(data: String) = HttpStatusCode.OK to """{"isSuccess":true,"data":$data}"""

    private fun defaults() {
        replies["/api/Catalogs/cities"] = { ok("""[{"address":{"slug":"minsk"},"name":"Минск"}]""") }
        replies["/api/Catalogs/beans"] = { ok("""[{"slug":"arabica","name":"Арабика"},{"slug":"x","address":{"slug":"public-x"},"name":"Икс"}]""") }
        replies["/api/Catalogs/equipments"] = { ok("""[{"slug":"marzocco","name":"La Marzocco"}]""") }
        replies["/api/Catalogs/brew-methods"] = { ok("""[{"slug":"v60","name":"V60"}]""") }
        replies["/api/Catalogs/shop-tags"] = { ok("""[{"slug":"wifi","name":"Wi-Fi"}]""") }
        replies["/api/menu/drinks"] = {
            ok("""{"drinks":[{"slug":"latte","nameRu":" ","nameEn":"Latte","sortOrder":2},{"slug":"espresso","nameRu":"Эспрессо","nameEn":"Espresso","sortOrder":1},{"slug":"flat","sortOrder":3}]}""")
        }
    }

    private fun source(): ContributionCatalogSource {
        val client = HttpClientFactory(MockEngine { request ->
            requests += request
            val (status, body) = replies[request.url.encodedPath]?.invoke() ?: (HttpStatusCode.NotFound to "{}")
            respond(body, status, json)
        }).api("https://api.example")
        return createContributionCatalogSource(client) { roasters }
    }

    private fun count(path: String) = requests.count { it.url.encodedPath == path }

    @Test fun theCatalogsMapEveryListAndKeepTheRoasterLogo() = runBlocking {
        defaults()
        val all = source().catalogs().getOrThrow()
        assertEquals(listOf("minsk"), all.cities.map { it.id })
        assertEquals(listOf("arabica", "public-x"), all.beans.map { it.id }, "the public address slug wins")
        assertEquals(listOf("marzocco"), all.equipment.map { it.id })
        assertEquals(listOf("v60"), all.brewMethods.map { it.id })
        assertEquals(listOf("wifi"), all.tags.map { it.id })
        assertEquals("https://logo", all.roasters.single().photoUrl)
        assertTrue(all.beans.all { it.photoUrl == null })
    }

    @Test fun aSuccessfulReadIsKeptAndAFailureIsRetried() = runBlocking {
        defaults()
        var healthy = false
        replies["/api/Catalogs/cities"] = { if (healthy) ok("""[{"address":{"slug":"minsk"},"name":"Минск"}]""") else HttpStatusCode.OK to """{"isSuccess":false,"message":"offline"}""" }
        val source = source()
        assertEquals("offline", source.catalogs().exceptionOrNull()?.message)
        healthy = true
        assertTrue(source.catalogs().isSuccess)
        val before = count("/api/Catalogs/cities")
        assertTrue(source.catalogs().isSuccess)
        assertEquals(before, count("/api/Catalogs/cities"), "a kept success is not read again")
    }

    @Test fun callersThatArriveTogetherShareOneLoad() = runBlocking {
        defaults()
        val source = source()
        val results = listOf(async { source.catalogs() }, async { source.catalogs() }, async { source.catalogs() }).map { it.await() }
        assertTrue(results.all { it.isSuccess })
        assertEquals(1, count("/api/Catalogs/beans"))
    }

    @Test fun aFailingRoasterCatalogFailsTheCatalogs() = runBlocking {
        defaults()
        roasters = Result.failure(IllegalStateException("roasters offline"))
        assertEquals("roasters offline", source().catalogs().exceptionOrNull()?.message)
    }

    @Test fun theDrinksAreSortedNamedRussianThenEnglishThenSlugAndKept() = runBlocking {
        defaults()
        val source = source()
        assertEquals(listOf("Эспрессо", "Latte", "flat"), source.drinks().getOrThrow().map { it.name })
        assertEquals(listOf("espresso", "latte", "flat"), source.drinks().getOrThrow().map { it.slug })
        assertEquals(1, count("/api/menu/drinks"))
    }

    private val shopBody = """{
        "name":"Cafe","description":"Text",
        "photos":[
          {"id":"p2","fullUrl":"https://full2","sortIndex":"2"},
          {"id":"p1","urls":{"thumbnail":"https://thumb1","fullscreen":"https://big1"},"sortIndex":1},
          {"id":"","fullUrl":"https://no-id"},
          {"id":"p3","fullUrl":"  "}],
        "roasters":[{"slug":"r","name":"R"}],"equipments":[{"slug":"e"}],"brewMethods":[{"slug":"m","address":{"slug":"public-m"}}],
        "shopTags":[{"slug":"wifi","name":"Wi-Fi"},{"slug":"wifi"},{"name":"no slug"}],"tags":[{"slug":"old"}],
        "shopContact":{"instagramLink":"https://instagram.com/cafe","email":"a@b.c","siteLink":"https://cafe.by","phoneNumber":"+375"},
        "menu":{
          "items":[{"slug":"flat","availability":"unknown"},{"slug":"latte","availability":"2","price":"4,5"},{"slug":"espresso","availability":"Present","price":3.2},{"slug":"mystery","availability":"1"}],
          "photos":[{"id":"m2","fullUrl":"https://menu-full2","sortIndex":2},{"id":"m1","urls":{"detail":"https://menu-detail","fullscreen":"https://menu-big"},"sortIndex":1},{"id":"m3"}]}}"""

    @Test fun theShopSnapshotCarriesTheEditablePartsInTheLegacyShape() = runBlocking {
        defaults()
        replies["/api/CoffeeShops/cafe-slug"] = { ok(shopBody) }
        val snapshot = source().shop("cafe-slug").getOrThrow()
        assertEquals("cafe-slug", snapshot.slug)
        assertEquals("Cafe", snapshot.title); assertEquals("Text", snapshot.description)
        assertEquals("+375", snapshot.contacts.phone); assertEquals("a@b.c", snapshot.contacts.email)
        assertEquals("https://cafe.by", snapshot.contacts.website); assertEquals("https://instagram.com/cafe", snapshot.contacts.instagram)
        assertEquals(listOf(ShopPhotoRef("p1", "https://thumb1"), ShopPhotoRef("p2", "https://full2")), snapshot.photos, "sorted, small variant, no-id and no-url photos dropped")
        assertEquals(listOf("wifi"), snapshot.tagIds, "shop tags first, distinct, slug-less dropped")
        assertEquals(listOf("r"), snapshot.roasterIds); assertEquals(listOf("e"), snapshot.equipmentIds); assertEquals(listOf("public-m"), snapshot.brewMethodIds)
        assertEquals(listOf(ShopPhotoRef("m1", "https://menu-detail"), ShopPhotoRef("m2", "https://menu-full2")), snapshot.menuPhotos.take(2))
    }

    @Test fun theMenuFollowsTheCatalogOrderWithUnknownRowsLast() = runBlocking {
        defaults()
        replies["/api/CoffeeShops/cafe-slug"] = { ok(shopBody) }
        val menu = source().shop("cafe-slug").getOrThrow().menu
        assertEquals(listOf("espresso", "latte", "flat", "mystery"), menu.map { it.slug })
        assertEquals(listOf(Availability.Present, Availability.Absent, Availability.Unknown, Availability.Present), menu.map { it.availability })
        assertEquals(listOf(3.2, 4.5, null, null), menu.map { it.price }, "a comma decimal is read")
    }

    @Test fun aFailedDrinksCatalogKeepsTheShopsOwnMenuOrder() = runBlocking {
        defaults()
        replies["/api/menu/drinks"] = { HttpStatusCode.InternalServerError to "{}" }
        replies["/api/CoffeeShops/cafe-slug"] = { ok(shopBody) }
        assertEquals(listOf("flat", "latte", "espresso", "mystery"), source().shop("cafe-slug").getOrThrow().menu.map { it.slug })
    }

    @Test fun theShopIsReadFreshAndItsSlugIsAnEncodedPathSegment() = runBlocking {
        defaults()
        replies["/api/CoffeeShops/a%2Fb"] = { ok("""{"name":"Odd"}""") }
        val source = source()
        assertEquals("Odd", source.shop("a/b").getOrThrow().title)
        source.shop("a/b")
        assertEquals(2, count("/api/CoffeeShops/a%2Fb"), "no snapshot cache")
        assertNull(source.shop("a/b").getOrThrow().description)
    }

    @Test fun aRejectedShopOrAnHttpErrorIsPassedOn() = runBlocking {
        defaults()
        replies["/api/CoffeeShops/gone"] = { HttpStatusCode.OK to """{"isSuccess":false,"message":"Not found"}""" }
        replies["/api/CoffeeShops/down"] = { HttpStatusCode.ServiceUnavailable to "{}" }
        val source = source()
        assertEquals("Not found", source.shop("gone").exceptionOrNull()?.message)
        assertFalse(source.shop("down").isSuccess)
    }
}
