package com.coffeepeek.data.mapper

import com.coffeepeek.api.service.ShopApiService
import com.coffeepeek.data.mapper.ShopMapper.toDomain
import com.coffeepeek.data.util.FileUrlResolver
import com.coffeepeek.domain.model.CheckInModerationState
import com.coffeepeek.domain.model.CheckInVisibility
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopCheckInsContractTest {
    @Test
    fun shopResponseReachesDomainWithPublicVisitsAndPersonalHistoryIntact() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals("/api/CoffeeShops/coffee", request.url.encodedPath)
            respond("""{"isSuccess":true,"message":null,"data":{
                "address":{"slug":"coffee","canonicalPath":"/coffee-shops/coffee","revision":1,"isAlias":false},
                "name":"Кофейня","checkInCount":23,
                "checkIns":[{
                    "id":"visit-1","shop":null,"author":null,"username":"Анна","shopName":"",
                    "text":"Отличный кофе","createdAtUtc":"2026-10-08T12:00:00Z","visitedAt":"2026-10-01T09:00:00Z",
                    "rating":{"place":4,"service":5,"coffee":5},"visibility":"Public","moderationState":"Approved",
                    "contentRevision":2,"drinkSlug":"cappuccino","drinkNameRu":"Капучино","drinkNameEn":"Cappuccino",
                    "helpfulCount":7,"isHelpfulByCurrentUser":true,
                    "photos":[
                        {"id":"later","sortIndex":1,"url":"/api/v1/check-ins/visit-1/photos/later"},
                        {"id":"first","sortIndex":0,"url":"https://cdn.example/first.jpg"},
                        {"id":"no-url","sortIndex":2,"storageKey":"must-not-invent-url"}
                    ]
                },{
                    "id":"visit-2","username":"Анна","text":"Повторное посещение","visibility":"Public",
                    "moderationState":"Approved","drinkSlug":"other","customDrinkName":"Эспрессо-тоник"
                }],
                "userCheckIns":[{"id":"private","text":"Личная заметка","visibility":"Private"}]
            }}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        try {
            val details = ShopApiService(client).getShopDetails("coffee").getOrThrow()
                .toDomain(FileUrlResolver("https://api.example/"))
            assertEquals(23, details.shop.reviewCount)
            assertEquals(listOf("visit-1", "visit-2"), details.checkIns.map { it.id })
            assertTrue(details.reviews.isEmpty())
            val visit = details.checkIns.first()
            assertNull(visit.shopAddress)
            assertNull(visit.authorAddress)
            assertEquals("Анна", visit.username)
            assertEquals("Кофейня", visit.shopName)
            assertEquals("Отличный кофе", visit.note)
            assertEquals("2026-10-08T12:00:00Z", visit.createdAt)
            assertEquals("2026-10-01T09:00:00Z", visit.visitedAt)
            assertEquals(5, visit.rating?.coffee)
            assertEquals(CheckInVisibility.Public, visit.visibility)
            assertEquals(CheckInModerationState.Approved, visit.moderationState)
            assertEquals(2, visit.contentRevision)
            assertEquals("Капучино", visit.drinkNameRu)
            assertEquals("Cappuccino", visit.drinkNameEn)
            assertEquals(7, visit.helpfulCount)
            assertTrue(visit.isHelpfulByCurrentUser)
            assertEquals(listOf("https://cdn.example/first.jpg", "https://api.example/api/v1/check-ins/visit-1/photos/later"), visit.photoUrls)
            assertEquals(visit.photoUrls, visit.photoThumbnailUrls)
            assertEquals("Эспрессо-тоник", details.checkIns[1].customDrinkName)
            assertEquals(CheckInVisibility.Private, details.userCheckIns.single().visibility)
        } finally { client.close() }
    }
}
