package com.coffeepeek.data.mapper

import com.coffeepeek.api.model.PublicAddressDto
import com.coffeepeek.api.model.response.shop.CoffeeShopDetailsDto
import com.coffeepeek.data.local.LocalFavoriteShopDto
import com.coffeepeek.data.mapper.ShopMapper.toDomain
import com.coffeepeek.data.util.FileUrlResolver
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.serialization.SerializationException

class PublicAddressMappingTest {
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
        val saved = LocalFavoriteShopDto.from(details.shop)
        val restored = Json.decodeFromString<LocalFavoriteShopDto>(Json.encodeToString(saved)).toDomain()
        assertEquals(details.shop.publicAddress, restored.shop.publicAddress)
    }

    @Test
    fun oldFavoriteHasNoPublicAddressAndGuidCannotReplaceItInPublicDto() {
        val old = Json.decodeFromString<LocalFavoriteShopDto>("""{"id":"611c3b59-c086-44bb-a4c5-c350be4ee7d9","title":"Saved shop"}""")
        assertNull(old.toDomain().shop.publicAddress)
        assertFailsWith<SerializationException> {
            Json { ignoreUnknownKeys = true }.decodeFromString<CoffeeShopDetailsDto>("""{"id":"611c3b59-c086-44bb-a4c5-c350be4ee7d9"}""")
        }
        assertEquals("petr", PublicAddressDto("petr", "/users/petr", 1, false).toDomain().slug)
    }
}
