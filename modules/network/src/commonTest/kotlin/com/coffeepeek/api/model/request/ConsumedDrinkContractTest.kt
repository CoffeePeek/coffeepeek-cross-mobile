package com.coffeepeek.api.model.request

import com.coffeepeek.api.model.response.ConsumedDrinkOptionDto
import com.coffeepeek.api.model.response.shop.RatingDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import kotlin.test.*

class ConsumedDrinkContractTest {
    private val json = Json { encodeDefaults = true }
    @Test fun omittedSelectionPreservesAndClearIsExplicit() {
        val req = UpdateReviewReq("id", "header", "comment", RatingDto())
        val omitted = json.parseToJsonElement(json.encodeToString(req)).jsonObject
        assertFalse("drinkSlug" in omitted)
        assertFalse("customDrinkName" in omitted)
        assertFalse("clearDrink" in omitted)
        val cleared = json.parseToJsonElement(json.encodeToString(req.copy(clearDrink = true))).jsonObject
        assertEquals(JsonPrimitive(true), cleared["clearDrink"])
        val other = json.parseToJsonElement(json.encodeToString(req.copy(drinkSlug = "other", customDrinkName = "Тоник"))).jsonObject
        assertEquals(JsonPrimitive("other"), other["drinkSlug"])
        assertEquals(JsonPrimitive("Тоник"), other["customDrinkName"])
        assertFalse("drinkNameRu" in other)
    }
    @Test fun selectionsAreSentOnCreateAndHistoricalNamesDecode() {
        val checkIn = json.parseToJsonElement(json.encodeToString(CreateCheckInReq(
            coffeeShopId = "shop", isPublic = false, visitedAt = "2026-10-02T10:00:00Z",
            drinkSlug = "other", customDrinkName = "Тоник",
        ))).jsonObject
        assertEquals(JsonPrimitive("other"), checkIn["drinkSlug"])
        assertEquals(JsonPrimitive("Тоник"), checkIn["customDrinkName"])
        val review = json.parseToJsonElement(json.encodeToString(SendReviewReq(
            "shop", "header", "comment", RatingDto(), drinkSlug = "cappuccino",
        ))).jsonObject
        assertEquals(JsonPrimitive("cappuccino"), review["drinkSlug"])
        assertFalse("customDrinkName" in review)
        val old = json.decodeFromString<com.coffeepeek.api.model.response.shop.ReviewDto>("""{"id":"old"}""")
        assertNull(old.drinkSlug)
        assertNull(old.drinkNameRu)
        val saved = json.decodeFromString<com.coffeepeek.api.model.response.CheckInDto>(
            """{"id":"saved","drinkSlug":"inactive","drinkNameRu":"Старое название","drinkNameEn":"Old name"}""",
        )
        assertEquals("Старое название", saved.drinkNameRu)
        assertEquals("Old name", saved.drinkNameEn)
    }
    @Test fun catalogSupportsNullCategoryForOther() {
        val options = json.decodeFromString<List<ConsumedDrinkOptionDto>>("""[{"slug":"espresso","category":1},{"slug":"other","category":null}]""")
        assertEquals("other", options.last().slug)
        assertNull(options.last().category)
    }
}
