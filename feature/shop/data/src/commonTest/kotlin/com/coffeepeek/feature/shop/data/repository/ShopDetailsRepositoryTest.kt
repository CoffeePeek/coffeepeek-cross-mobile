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

class ShopDetailsRepositoryTest {
    @Test fun mapsReviewsAndCheckInsWithFileKeysAndFlexibleRatings() = runBlocking {
        val engine = MockEngine {
            respond("""{
              "isSuccess":true,
              "data":{"shopDto":{
                "id":"shop-1",
                "reviews":[{
                  "id":"review-1","moderationReviewId":"moderation-1","userId":"user-1",
                  "coffeeShopId":"shop-1","username":"Alex","header":"Great","comment":"Coffee",
                  "rating":{"place":"5","service":4,"coffee":4.9},
                  "photos":[{"storageKey":"reviews/photo.jpg"},{"fullUrl":"https://cdn.example.com/second.jpg"}],
                  "createdAtUtc":"2026-10-01T12:00:00Z","helpfulCount":3,"isHelpfulByCurrentUser":true
                }],
                "userCheckIns":[{
                  "id":"checkin-1","userId":"user-1","shopId":"shop-1","note":"Visited",
                  "createdAt":"2026-10-01","visitedAt":"2026-09-30","reviewId":"review-1",
                  "photos":[{"storageKey":"checkins/photo.jpg","urls":{"thumbnail":"https://cdn.example.com/thumb.jpg"}}],
                  "rating":{"place":"5","service":"4","coffee":"3"}
                }]
              }}
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
            val checkIn = details.userCheckIns.single()
            assertEquals("review-1", checkIn.reviewId)
            assertEquals("Visited", checkIn.note)
            assertEquals("https://files.example.com/api/file/checkins/photo.jpg", checkIn.photoUrls.single())
            assertEquals("https://cdn.example.com/thumb.jpg", checkIn.photoThumbnailUrls.single())
            assertEquals(3, checkIn.rating?.coffee)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test fun mapsMenuAndShiftsUtcScheduleFromOneRequest() = runBlocking {
        var calls = 0
        val engine = MockEngine {
            calls++
            respond("""{
              "isSuccess":true,
              "data":{
                "shopDto":{
                  "id":"shop-1",
                  "schedules":[
                    {"dayOfWeek":"monday","intervals":[{"openTime":"22:30","closeTime":"23:30"}]},
                    {"dayOfWeek":2,"isClosed":true,"intervals":[{"openTime":"09:00","closeTime":"10:00"}]}
                  ]
                },
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
              "data":{"shopDto":{
                "id":"shop-1","name":"Coffee","description":"  Fresh coffee  ",
                "location":{"address":" Main street ","latitude":53.9,"longitude":27.5},
                "rating":4.6,"reviewCount":12,"isOpen":true,
                "photos":[
                  {"id":"first","fullUrl":"https://photo/original","urls":{"detail":"https://photo/hero","fullscreen":"https://photo/full"}},
                  {"id":"second","fullUrl":"https://photo/second"},
                  {"id":"missing"}
                ],
                "coffeeBeans":[{"id":"bean-1","name":"  Эфиопия  "},{"id":"empty","name":" "}],
                "roasters":[{"id":"roaster-1","name":"  Roaster  ","photoUrl":" https://photo/roaster "}],
                "equipments":[{"id":"equipment-1","name":" V60 "}],
                "brewMethods":[{"id":"brew-1","name":" Эспрессо ","slug":"espresso"}],
                "shopTags":[{"id":"tag-1","name":" Wi-Fi ","slug":"wifi"},"Эспрессо"],
                "tags":["Ignored fallback"],
                "shopContact":{"phoneNumber":" +375 29 123 45 67 ","email":" a@example.com ","siteLink":" https://example.com ","instagramLink":" @coffee "},
                "menu":{"photos":[]},"otherField":"ignored"
              }}
            }""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(engine).api("https://example.com")
        try {
            val details = createShopDetailsRepository(client, "https://example.com") { 0 }
                .getDetails("shop-1").getOrThrow()
            val overview = details.overview
            assertEquals("shop-1", overview.id)
            assertEquals("Coffee", overview.title)
            assertEquals("Fresh coffee", overview.description)
            assertEquals("Main street", overview.address)
            assertEquals(53.9, overview.latitude)
            assertEquals(27.5, overview.longitude)
            assertEquals(4.6, overview.rating)
            assertEquals(12, overview.reviewCount)
            assertTrue(overview.isOpen)
            assertEquals(listOf("first", "second"), overview.photos.map { it.id })
            assertEquals("https://photo/hero", overview.photos.first().previewUrl)
            assertEquals("https://photo/full", overview.photos.first().fullUrl)
            assertEquals("https://photo/second", overview.photos.last().previewUrl)
            assertEquals(listOf("Эфиопия"), details.coffee.beans)
            assertEquals("Roaster", details.coffee.roasters.single().name)
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
                """{"isSuccess":true,"data":{"shopDto":{"name":"Unnamed rating"}}}"""
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
