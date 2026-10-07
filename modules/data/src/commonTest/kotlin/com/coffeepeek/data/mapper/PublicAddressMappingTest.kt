package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.PublicAddressDto
import com.coffeepeek.api.model.response.shop.CoffeeShopDetailsDto
import com.coffeepeek.api.model.response.shop.CatalogItemDto
import com.coffeepeek.data.local.LocalFavoriteShopDto
import com.coffeepeek.data.mapper.ShopMapper.toDomain
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.CoffeeShopType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.SerializationException

class PublicAddressMappingTest {
    @Test
    fun mapsRoasterCountsAndCoverPhotoFromCatalogContract() {
        val json = Json { ignoreUnknownKeys = true }
        val fileUrls = FileUrlResolver("https://api.example")
        val dto = json.decodeFromString<CatalogItemDto>(
            """{
                "address":{"slug":"roast","canonicalPath":"/roasters/roast","revision":1,"isAlias":false},
                "name":null,"photoUrl":null,
                "coffeeShopsCount":"7","coffeeProductsCount":12,"availableCoffeeProducts":"4",
                "tags":[
                    {"slug":"delivery","name":"Доставка","description":null,"sortOrder":"2"},
                    {"slug":"specialty","name":"Specialty","description":"Спешелти кофе","sortOrder":1}
                ],
                "coverPhoto":{"fullUrl":null,"sortIndex":"0","urls":{"card":"https://m/card.jpg"}}
            }""",
        )
        val roaster = dto.toDomain(fileUrls)
        assertEquals("roast", roaster.id)
        assertEquals("", roaster.name)
        assertEquals(7, roaster.coffeeShopsCount)
        assertEquals(12, roaster.coffeeProductsCount)
        assertEquals(4, roaster.availableCoffeeProducts)
        assertEquals(listOf("specialty", "delivery"), roaster.tags.map { it.slug })
        assertEquals("Спешелти кофе", roaster.tags.first().description)
        assertEquals("https://m/card.jpg", roaster.photoUrl)
        assertEquals("https://m/legacy.jpg", dto.copy(coverPhoto = null, photoUrl = "https://m/legacy.jpg").toDomain(fileUrls).photoUrl)
        for (payload in listOf("{}", """{"coffeeShopsCount":0,"coffeeProductsCount":"0"}""", """{"coffeeShopsCount":null,"coffeeProductsCount":null}""")) {
            val empty = json.decodeFromString<CatalogItemDto>(payload).toDomain(fileUrls)
            assertEquals(0, empty.coffeeShopsCount)
            assertEquals(0, empty.coffeeProductsCount)
            assertEquals(0, empty.availableCoffeeProducts)
            assertTrue(empty.tags.isEmpty())
        }
    }

    @Test
    fun keepsRoastersWithNullableAddressAndTagFields() {
        val dto = Json.decodeFromString<CatalogItemDto>(
            """{
                "address":{"slug":null,"canonicalPath":null,"revision":"0","isAlias":false},
                "tags":[{"slug":null,"name":null,"description":null,"sortOrder":"0"}],
                "availableCoffeeProducts":null
            }""",
        )
        val roaster = dto.toDomain(FileUrlResolver("https://api.example"))
        assertEquals("", roaster.id)
        assertNull(roaster.address)
        assertEquals(0, roaster.availableCoffeeProducts)
        assertEquals("", roaster.tags.single().name)
    }

    @Test
    fun preservesDrinkSnapshotsInShopCheckInsAndReviews() {
        val dto = Json.decodeFromString<CoffeeShopDetailsDto>(
            """{
                "address":{"slug":"shop","canonicalPath":"/coffee-shops/shop","revision":1,"isAlias":false},
                "userCheckIns":[
                    {"id":"catalog","drinkSlug":"inactive-drink","drinkNameRu":"Сохранённое название","drinkNameEn":"Saved name"},
                    {"id":"custom","drinkSlug":"other","customDrinkName":"Эспрессо-тоник"},
                    {"id":"legacy"}
                ],
                "reviews":[{"id":"review","drinkSlug":"other","customDrinkName":"Эспрессо-тоник"}]
            }""",
        )
        val details = dto.toDomain(FileUrlResolver("https://api.example"))
        assertEquals("inactive-drink", details.userCheckIns[0].drinkSlug)
        assertEquals("Сохранённое название", details.userCheckIns[0].drinkNameRu)
        assertEquals("Saved name", details.userCheckIns[0].drinkNameEn)
        assertEquals("Эспрессо-тоник", details.userCheckIns[1].customDrinkName)
        assertNull(details.userCheckIns[2].drinkSlug)
        assertEquals("Эспрессо-тоник", details.reviews.single().customDrinkName)
    }

    @Test
    fun mapsSlugsAndPreservesCanonicalAddressInFavorites() {
        val dto = Json.decodeFromString<CoffeeShopDetailsDto>(
            """{
                "address":{"slug":"26-october-16","canonicalPath":"/coffee-shops/26-october-16","revision":2,"isAlias":true},
                "city":{"slug":"minsk","canonicalPath":"/cities/minsk","revision":1,"isAlias":false},
                "tags":[{"slug":"dog-friendly","name":"С собакой"}],
                "beans":[{"slug":"arabica","name":"Arabica"}],
                "reviews":[{"id":"review-guid","author":null,"shop":null}],
                "userCheckIns":[{"id":"check-in-guid","shop":null}]
            }""",
        )
        val details = dto.toDomain(FileUrlResolver("https://api.example"))
        assertEquals("26-october-16", details.shop.id)
        assertEquals("minsk", details.cityId)
        assertEquals("dog-friendly", details.tagItems.single().id)
        assertEquals(listOf("Arabica"), details.coffeeBeans)
        assertEquals("", details.reviews.single().userId)
        assertEquals("", details.reviews.single().shopId)
        assertEquals("", details.userCheckIns.single().shopId)
        val saved = LocalFavoriteShopDto.from(details.shop.copy(type = CoffeeShopType.SPECIALTY, isNew = true))
        val restored = Json.decodeFromString<LocalFavoriteShopDto>(Json.encodeToString(saved)).toDomain()
        assertEquals(details.shop.publicAddress, restored.shop.publicAddress)
        assertEquals(CoffeeShopType.SPECIALTY, restored.shop.type)
        assertEquals(true, restored.shop.isNew)
    }

    @Test
    fun oldFavoriteHasNoPublicAddressAndGuidCannotReplaceItInPublicDto() {
        val old = Json.decodeFromString<LocalFavoriteShopDto>("""{"id":"611c3b59-c086-44bb-a4c5-c350be4ee7d9","title":"Saved shop"}""")
        assertNull(old.toDomain().shop.publicAddress)
        assertEquals(CoffeeShopType.COFFEE_BAR, old.toDomain().shop.type)
        assertEquals(false, old.toDomain().shop.isNew)
        assertFailsWith<SerializationException> {
            Json { ignoreUnknownKeys = true }.decodeFromString<CoffeeShopDetailsDto>("""{"id":"611c3b59-c086-44bb-a4c5-c350be4ee7d9"}""")
        }
        assertEquals("petr", PublicAddressDto("petr", "/users/petr", 1, false).toDomain().slug)
    }
}
