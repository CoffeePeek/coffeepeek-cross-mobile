package com.coffeepeek.api.model.request

import com.coffeepeek.api.model.response.shop.RatingDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlin.test.*

class CheckInRequestContractTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun createUsesV1FieldsAndAttachmentSizeWithoutPhotoId() {
        val payload = json.parseToJsonElement(json.encodeToString(CreateCheckInReq(
            coffeeShopSlug = "example-coffee", text = "Хороший кофе",
            rating = RatingDto(4, 4, 5), visibility = "Public", visitedAt = "2026-10-07T09:00:00Z",
            photos = listOf(UploadedPhotoReq("coffee.jpg", "image/jpeg", "check-ins/key.jpg", 12)),
        ))).jsonObject
        assertEquals(JsonPrimitive("example-coffee"), payload["coffeeShopSlug"])
        assertEquals(JsonPrimitive("Хороший кофе"), payload["text"])
        assertEquals(JsonPrimitive("Public"), payload["visibility"])
        assertEquals(JsonPrimitive(5), payload["rating"]!!.jsonObject["coffee"])
        val photo = payload["photos"]!!.jsonArray.single().jsonObject
        assertEquals(JsonPrimitive(12), photo["size"])
        assertFalse("sizeBytes" in photo)
        assertFalse("photoId" in photo)
        for (obsolete in listOf("shop", "coffeeShopId", "isPublic", "note", "header", "author")) assertFalse(obsolete in payload)
    }

    @Test
    fun editingFullyReplacesDrinkAndDoesNotSendImmutableVisitFields() {
        val source = UpdateCheckInReq("Обновлённый текст", RatingDto(5, 4, 5), "cappuccino")
        val selected = json.parseToJsonElement(json.encodeToString(source)).jsonObject
        assertEquals(JsonPrimitive("cappuccino"), selected["drinkSlug"])
        val cleared = json.parseToJsonElement(json.encodeToString(source.copy(drinkSlug = null))).jsonObject
        assertEquals(JsonNull, cleared["drinkSlug"])
        assertEquals(JsonNull, cleared["customDrinkName"])
        for (immutable in listOf("photos", "visitedAt", "coffeeShopSlug", "visibility", "clearDrink")) assertFalse(immutable in cleared)
        assertEquals(JsonPrimitive("Private"), json.parseToJsonElement(json.encodeToString(CheckInVisibilityReq("Private"))).jsonObject["visibility"])
    }
}
