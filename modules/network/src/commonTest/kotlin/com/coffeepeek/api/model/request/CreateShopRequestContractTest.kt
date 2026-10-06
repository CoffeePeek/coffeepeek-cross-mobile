package com.coffeepeek.api.model.request

import com.coffeepeek.api.utils.JsonExt
import kotlin.test.Test
import kotlin.test.assertContains

class CreateShopRequestContractTest {

    @Test
    fun requestKeepsLocationContactsAndUtcSchedule() {
        val encoded = JsonExt.json.encodeToString(
            CreateShopReq.serializer(),
            CreateShopReq(
                name = "Coffee Point",
                address = "Lenina 1",
                cityId = "minsk",
                coffeeBeanIds = listOf("arabica"),
                equipmentIds = listOf("la-marzocco-linea"),
                roasterIds = listOf("coffee-roaster"),
                brewMethodIds = listOf("v60"),
                latitude = 53.9023,
                longitude = 27.5619,
                shopContact = CreateShopContactReq(phoneNumber = "+375291234567"),
                schedules = listOf(
                    ScheduleReq(
                        dayOfWeek = "Monday",
                        isClosed = false,
                        intervals = listOf(ScheduleIntervalReq("06:00", "18:00")),
                    ),
                ),
            ),
        )

        assertContains(encoded, "\"city\":\"minsk\"")
        assertContains(encoded, "\"beans\":[\"arabica\"]")
        assertContains(encoded, "\"equipments\":[\"la-marzocco-linea\"]")
        assertContains(encoded, "\"roasters\":[\"coffee-roaster\"]")
        assertContains(encoded, "\"brewMethods\":[\"v60\"]")
        assertContains(encoded, "\"latitude\":53.9023")
        assertContains(encoded, "\"longitude\":27.5619")
        assertContains(encoded, "\"shopContact\":{\"phoneNumber\":\"+375291234567\"")
        assertContains(encoded, "\"dayOfWeek\":\"Monday\"")
        assertContains(encoded, "\"openTime\":\"06:00\"")
        assertContains(encoded, "\"closeTime\":\"18:00\"")
    }
}
