package com.coffeepeek.feature.favorites.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoritesRouteTest {
    @Test fun routeRoundTripsWithoutImplementationArguments() {
        assertEquals(FavoritesRoute, Json.decodeFromString<FavoritesRoute>(Json.encodeToString(FavoritesRoute.serializer(), FavoritesRoute)))
    }
}
