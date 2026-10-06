package com.coffeepeek.api.model.request

import com.coffeepeek.api.utils.JsonExt
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ShopChangeRequestContractTest {

    private val json = JsonExt.json

    @Test
    fun createDescriptionBodyUsesSectionAndPayloadKeys() {
        val encoded = json.encodeToString(
            CreateShopChangeRequestBody.serializer(),
            CreateShopChangeRequestBody(
                shopId = "26-october-16",
                section = ShopChangeSectionDto.Description,
                payload = ShopChangePayloadDto(description = "Новое описание кофейни"),
            ),
        )
        assertContains(encoded, "\"shop\":\"26-october-16\"")
        assertContains(encoded, "\"section\":\"Description\"")
        assertContains(encoded, "\"description\":\"Новое описание кофейни\"")
    }

    @Test
    fun contactsPayloadKeepsNullFieldsToClearValues() {
        val encoded = json.encodeToString(
            ShopChangePayloadDto.serializer(),
            ShopChangePayloadDto(
                contacts = ShopChangeContactsDto(
                    phoneNumber = "+375291234567",
                    email = null,
                    siteLink = null,
                    instagramLink = "@coffee",
                ),
            ),
        )
        assertContains(encoded, "\"phoneNumber\":\"+375291234567\"")
        assertContains(encoded, "\"email\":null")
        assertContains(encoded, "\"instagramLink\":\"@coffee\"")
    }

    @Test
    fun requestDtoDecodesCamelCaseEnums() {
        val decoded = json.decodeFromString(
            ShopChangeRequestDto.serializer(),
            """
            {
              "id": "req-1",
              "shop": {"slug":"shop-1","canonicalPath":"/coffee-shops/shop-1","revision":1,"isAlias":false},
              "section": "Tags",
              "payload": { "tags": ["tag-1"] },
              "status": "Pending",
              "reviewedAtUtc": null,
              "rejectionReason": null,
              "createdAtUtc": "2026-09-22T08:00:00Z",
              "updatedAtUtc": null
            }
            """.trimIndent(),
        )
        assertEquals(ShopChangeSectionDto.Tags, decoded.section)
        assertEquals(ModerationStatusDto.Pending, decoded.status)
        assertEquals(listOf("tag-1"), decoded.payload.tagIds)
    }
}
