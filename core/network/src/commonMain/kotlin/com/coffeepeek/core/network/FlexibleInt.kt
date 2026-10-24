package com.coffeepeek.core.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * The server sends counters and indexes as a number, a numeric string or a decimal (`3`, `"3"`, `3.0`). This reads all of them as an Int, like the
 * legacy FlexibleInt serializer; anything else (null, text, an object) is null.
 */
fun JsonElement?.flexibleIntOrNull(): Int? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.intOrNull ?: primitive.content.trim().toIntOrNull() ?: primitive.doubleOrNull?.takeIf { it.isFinite() }?.toInt()
}
