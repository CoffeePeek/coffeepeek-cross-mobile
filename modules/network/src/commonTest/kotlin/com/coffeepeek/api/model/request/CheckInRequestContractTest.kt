package com.coffeepeek.api.model.request

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CheckInRequestContractTest {

    @Test
    fun publicCheckInSerializesHeaderFromApiContract() {
        val payload = Json.encodeToString(
            CreateCheckInReq(
                coffeeShopId = "26-october-16",
                isPublic = true,
                visitedAt = "2026-09-06T10:00:00Z",
                header = "Отличное место",
                note = "Описание публичного чекина",
            )
        )

        assertContains(payload, "\"shop\":\"26-october-16\"")
        assertFalse("coffeeShopId" in payload)
        assertContains(payload, "\"header\":\"Отличное место\"")
        assertContains(payload, "\"note\":\"Описание публичного чекина\"")
    }

    @Test
    fun absentHeaderIsNotSentForPrivateCheckIn() {
        val payload = Json.encodeToString(
            CreateCheckInReq(
                coffeeShopId = "26-october-16",
                isPublic = false,
                visitedAt = "2026-09-06T10:00:00Z",
            )
        )

        assertFalse("\"header\"" in payload)
    }
}
