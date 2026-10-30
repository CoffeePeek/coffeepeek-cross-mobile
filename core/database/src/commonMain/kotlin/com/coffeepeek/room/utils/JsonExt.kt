package com.coffeepeek.room.utils

import kotlinx.serialization.json.Json

object JsonExt {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

}