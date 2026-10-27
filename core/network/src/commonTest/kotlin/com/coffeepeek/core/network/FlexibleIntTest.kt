package com.coffeepeek.core.network

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FlexibleIntTest {
    @Test fun aNumberAStringAndADecimalAreTheSameInt() {
        assertEquals(3, JsonPrimitive(3).flexibleIntOrNull())
        assertEquals(3, JsonPrimitive("3").flexibleIntOrNull())
        assertEquals(3, JsonPrimitive(" 3 ").flexibleIntOrNull())
        assertEquals(3, JsonPrimitive(3.0).flexibleIntOrNull())
        assertEquals(-2, JsonPrimitive(-2.9).flexibleIntOrNull())
    }

    @Test fun nullTextObjectsAndNonFiniteNumbersAreNull() {
        assertNull(null.flexibleIntOrNull())
        assertNull(JsonNull.flexibleIntOrNull())
        assertNull(JsonPrimitive("many").flexibleIntOrNull())
        assertNull(JsonObject(emptyMap()).flexibleIntOrNull())
        assertNull(JsonPrimitive(Double.NaN).flexibleIntOrNull())
    }
}
