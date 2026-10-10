package com.coffeepeek.feature.shop.data.repository

import com.coffeepeek.core.network.HttpClientFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.coffeepeek.feature.shop.domain.model.ShopCheckInVisibility
import com.coffeepeek.feature.shop.domain.model.ShopCheckInModerationState

class ShopDetailsRepositoryTest {
    @Test fun mapsCurrentPublicAndPersonalCheckInsWithoutInventingPhotoUrls() = runBlocking {
        val engine = MockEngine {
            respond("""{
              "isSuccess":true,
              "data":{
                "address":{"slug":"shop-1","canonicalPath":"/coffee-shops/shop-1"},
                "checkInCount":"23",
                "reviews":[{
                  "id":"review-1","moderationReviewId":"moderation-1","userId":"user-1",
                  "coffeeShopId":"shop-1","username":"Alex","header":"Great","comment":"Coffee",
                  "rating":{"place":"5","service":4,"coffee":4.9},
                  "photos":[{"storageKey":"reviews/photo.jpg"},{"fullUrl":"https://cdn.example.com/second.jpg"}],
                  "createdAtUtc":"2026-10-01T12:00:00Z","helpfulCount":3,"isHelpfulByCurrentUser":true
                }],
                "checkIns":[{
                  "id":"checkin-1","author":{"slug":"user-1"},"shop":{"slug":"shop-1"},"username":"Alex","text":"Visited",
                  "createdAtUtc":"2026-10-01","visitedAt":"2026-09-30",
                  "photos":[{"sortIndex":"2","url":"/api/v1/check-ins/checkin-1/photos/second"},
                    {"sortIndex":1,"url":"https://cdn.example.com/first.jpg"},
                    {"sortIndex":3,"storageKey":"must-not-invent-url"}],
                  "rating":{"place":"5","service":"4","coffee":"3"},
                  "drinkSlug":"other","customDrinkName":"Espresso tonic","drinkNameRu":"Напиток","drinkNameEn":"Drink",
                  "visibility":"Public","moderationState":"Approved","contentRevision":"2",
                  "helpfulCount":"7","isHelpfulByCurrentUser":true
                }],
                "userCheckIns":[{
                  "id":"private","text":"Private note","visibility":"Private","moderationState":"Rejected",
                  "rejectionReason":"Needs editing"
                }]
              }
            }""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val details = createShopDetailsRepository(client, "https://files.example.com/") { 0 }
                .getDetails("shop-1").getOrThrow()
            val review = details.reviews.single()
            assertEquals("moderation-1", review.moderationReviewId)
            assertEquals(5, review.rating.place)
            assertEquals(4, review.rating.coffee)
            assertEquals(3, review.helpfulCount)
            assertTrue(review.isHelpfulByCurrentUser)
            assertEquals(listOf("https://files.example.com/api/file/reviews/photo.jpg",
                "https://cdn.example.com/second.jpg"), review.photoUrls)
            assertEquals(23, details.overview.reviewCount)
            val checkIn = details.checkIns.single()
            assertEquals("user-1", checkIn.userId)
            assertEquals("shop-1", checkIn.shopId)
            assertEquals("Alex", checkIn.username)
            assertNull(checkIn.reviewId)
            assertEquals("Visited", checkIn.note)
            assertEquals(listOf("https://cdn.example.com/first.jpg",
                "https://files.example.com/api/v1/check-ins/checkin-1/photos/second"), checkIn.photoUrls)
            assertEquals(checkIn.photoUrls, checkIn.photoThumbnailUrls)
            assertEquals(3, checkIn.rating?.coffee)
            assertEquals("other", checkIn.drinkSlug)
            assertEquals("Espresso tonic", checkIn.customDrinkName)
            assertEquals("Напиток", checkIn.drinkNameRu)
            assertEquals("Drink", checkIn.drinkNameEn)
            assertEquals(ShopCheckInVisibility.Public, checkIn.visibility)
            assertEquals(ShopCheckInModerationState.Approved, checkIn.moderationState)
            assertEquals(2, checkIn.contentRevision)
            assertEquals(7, checkIn.helpfulCount)
            assertTrue(checkIn.isHelpfulByCurrentUser)
            val personal = details.userCheckIns.single()
            assertEquals("Private note", personal.note)
            assertEquals(ShopCheckInVisibility.Private, personal.visibility)
            assertEquals(ShopCheckInModerationState.Rejected, personal.moderationState)
            assertEquals("Needs editing", personal.rejectionReason)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun mapsMenuAndShiftsUtcScheduleWithOptionalCatalog() = runBlocking {
        var calls = 0
        val engine = MockEngine {
            if (it.url.encodedPath == "/api/menu/drinks") return@MockEngine respond(
                """{"isSuccess":true,"data":{"drinks":[]}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
            calls++
            respond("""{
              "isSuccess":true,
              "data":{
                  "address":{"slug":"shop-1"},
                  "schedules":[
                    {"dayOfWeek":"monday","intervals":[{"openTime":"22:30","closeTime":"23:30"}]},
                    {"dayOfWeek":2,"isClosed":true,"intervals":[{"openTime":"09:00","closeTime":"10:00"}]}
                  ],
                "menu":{
                  "currency":"",
                  "items":[
                    {"slug":"flat-white","nameRu":"Флэт уайт","category":"Coffee","availability":"Present","price":"5,50","volumeMl":"250","currency":""},
                    {"slug":"filter","availability":"Absent","price":7}
                  ],
                  "photos":[{"id":"second","fullUrl":"https://photo/2","sortIndex":2},{"id":"first","fullUrl":"https://photo/1","sortIndex":1}]
                }
              }
            }""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val details = createShopDetailsRepository(client, "https://example.com") { 180 }
                .getDetails("shop-1").getOrThrow()
            assertEquals(1, calls)
            assertEquals("BYN", details.menu?.currency)
            assertEquals(5.5, details.menu?.items?.first()?.price)
            assertEquals(250, details.menu?.items?.first()?.volumeMl)
            assertEquals("BYN", details.menu?.items?.first()?.currency)
            assertEquals(listOf("first", "second"), details.menu?.photos?.map { it.id })
            assertEquals("01:30", details.schedules[2].intervals.single().openTime)
            assertEquals("02:30", details.schedules[2].intervals.single().closeTime)
            assertEquals(true, details.schedules[1].isClosed)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun mapsHeaderFieldsAndPhotoVariantsWithoutLoadingLegacyModels() = runBlocking {
        val engine = MockEngine {
            respond("""{
              "isSuccess":true,
              "data":{
                "address":{"slug":"shop-1","canonicalPath":"/coffee-shops/shop-1"},"name":"Coffee","description":"  Fresh coffee  ",
                "location":{"address":" Main street ","latitude":53.9,"longitude":27.5},
                "rating":4.6,"checkInCount":12,"isOpen":true,"priceRange":2,
                "photos":[
                  {"id":"first","fullUrl":"https://photo/original","urls":{"detail":"https://photo/hero","fullscreen":"https://photo/full"}},
                  {"id":"second","fullUrl":"https://photo/second"},
                  {"id":"missing"}
                ],
                "beans":[{"slug":"bean-1","name":"  Эфиопия  "},{"slug":"empty","name":" "}],
                "roasters":[{"id":"obsolete-id","slug":"old-slug","address":{"slug":"roaster-1","canonicalPath":"/roasters/roaster-1"},"name":"  Roaster  ","photoUrl":" https://photo/roaster "}],
                "equipments":[{"id":"equipment-1","name":" V60 "}],
                "brewMethods":[{"id":"brew-1","name":" Эспрессо ","slug":"espresso"}],
                "shopTags":[{"id":"tag-1","name":" Wi-Fi ","slug":"wifi"},"Эспрессо"],
                "tags":["Ignored fallback"],
                "shopContact":{"phoneNumber":" +375 29 123 45 67 ","email":" a@example.com ","siteLink":" https://example.com ","instagramLink":" @coffee "},
                "menu":{"photos":[]},"otherField":"ignored"
              }
            }""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val details = createShopDetailsRepository(client, "https://example.com") { 0 }
                .getDetails("shop-1").getOrThrow()
            val overview = details.overview
            assertEquals("shop-1", overview.id)
            assertEquals("/coffee-shops/shop-1", overview.canonicalPath)
            assertEquals("Coffee", overview.title)
            assertEquals("Fresh coffee", overview.description)
            assertEquals("Main street", overview.address)
            assertEquals(53.9, overview.latitude)
            assertEquals(27.5, overview.longitude)
            assertEquals(4.6, overview.rating)
            assertEquals(12, overview.reviewCount)
            assertEquals("$$", overview.priceRange)
            assertTrue(overview.isOpen)
            assertEquals(listOf("first", "second"), overview.photos.map { it.id })
            assertEquals("https://photo/hero", overview.photos.first().previewUrl)
            assertEquals("https://photo/full", overview.photos.first().fullUrl)
            assertEquals("https://photo/second", overview.photos.last().previewUrl)
            assertEquals(listOf("Эфиопия"), details.coffee.beans)
            assertEquals("Roaster", details.coffee.roasters.single().name)
            assertEquals("roaster-1", details.coffee.roasters.single().id)
            assertEquals("/roasters/roaster-1", details.coffee.roasters.single().canonicalPath)
            assertEquals("https://photo/roaster", details.coffee.roasters.single().photoUrl)
            assertEquals(listOf("V60"), details.coffee.equipment)
            assertEquals("+375 29 123 45 67", details.contact?.phone)
            assertEquals("a@example.com", details.contact?.email)
            assertEquals("https://example.com", details.contact?.website)
            assertEquals("@coffee", details.contact?.instagram)
            assertEquals(listOf("Эспрессо", "Wi-Fi"), details.features.map { it.name })
            assertEquals("wifi", details.features[1].slug)
            assertEquals(true, details.features[0].isBrewMethod)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun absentOptionalFieldsAndRejectedResponseRemainExplicit() = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond(if (calls == 1) {
                """{"isSuccess":true,"data":{"address":{"slug":"shop-1"},"name":"Unnamed rating"}}"""
            } else {
                """{"isSuccess":false,"data":null}"""
            }, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val repository = createShopDetailsRepository(client, "https://example.com") { 0 }
            val details = repository.getDetails("shop-1").getOrThrow()
            val overview = details.overview
            assertEquals("shop-1", overview.id)
            assertNull(overview.rating)
            assertNull(overview.address)
            assertTrue(overview.photos.isEmpty())
            assertNull(details.menu)
            assertTrue(details.schedules.isEmpty())
            assertTrue(details.coffee.beans.isEmpty())
            assertTrue(details.coffee.roasters.isEmpty())
            assertTrue(details.coffee.equipment.isEmpty())
            assertNull(details.contact)
            assertTrue(details.features.isEmpty())
            assertTrue(details.reviews.isEmpty())
            assertTrue(details.userCheckIns.isEmpty())
            assertTrue(repository.getDetails("shop-1").isFailure)
        } finally {
            client.close()
            engine.close()
        }
    }
}
