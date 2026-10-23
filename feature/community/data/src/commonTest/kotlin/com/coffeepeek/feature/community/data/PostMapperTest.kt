package com.coffeepeek.feature.community.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.community.data.repository.createCommunityRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostMapperTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private suspend fun one(body: String) = createCommunityRepository(
        HttpClientFactory(MockEngine { respond("""{"isSuccess":true,"data":{"items":[{"publishedAtUtc":"2026-10-13T10:00:00Z","checkIn":$body}]}}""", headers = json) }).api("https://api.example"),
        "https://api.example/",
    ).feed(20).getOrThrow().entries.single().post

    @Test fun aHomePreparationKeepsItsKindRecipeAndPhotoRouteWithoutAShopOrRating() = runBlocking {
        val post = one("""{"id":"home-1","kind":"home","shop":null,"rating":null,"visibility":"Public","moderationState":"Approved",
            "authorAvatarUrl":"/api/Photos/avatar/author.jpg","home":{"preparedOn":"2026-10-13","brewMethodCode":"hario-v60","beanName":"My beans","doseGrams":18.5,"brewDurationSeconds":120,"tasteScore":5},
            "photos":[{"url":"/api/v1/check-ins/home-1/photos/photo-1"}]}""")
        assertTrue(post.isHome); assertEquals("", post.shopSlug); assertNull(post.rating)
        assertEquals("hario-v60", post.home?.brewMethodCode); assertEquals("My beans", post.home?.beanName); assertEquals(18.5, post.home?.doseGrams); assertEquals(5, post.home?.tasteScore)
        assertEquals(listOf("https://api.example/api/v1/check-ins/home-1/photos/photo-1"), post.photos.originals)
        assertEquals("https://api.example/api/Photos/avatar/author.jpg", post.author.avatarUrl)
    }

    @Test fun aShopVisitNeverCarriesARecipeEvenWhenTheServerSendsOne() = runBlocking {
        val post = one("""{"id":"visit","kind":"Shop","home":{"preparedOn":"2026-10-13","beanName":"Ignored"}}""")
        assertFalse(post.isHome); assertNull(post.home)
        val recipeless = one("""{"id":"home-2","kind":"Home"}""")
        assertTrue(recipeless.isHome); assertNull(recipeless.home) // a home post without a recipe still is a home post
    }

    @Test fun photosAreOrderedSkipUnusableUrlsAndNeverGuessAPathFromAStorageKey() = runBlocking {
        val post = one("""{"id":"v","photos":[
            {"sortIndex":"1","url":"https://api.example/protected-second"},{"sortIndex":0,"url":"/api/v1/check-ins/v/photos/first"},{"sortIndex":2,"url":"","storageKey":"never-public.jpg"}]}""")
        assertEquals(listOf("https://api.example/api/v1/check-ins/v/photos/first", "https://api.example/protected-second"), post.photos.originals)
        assertEquals(post.photos.originals, post.photos.thumbnails) // no variants: the original everywhere
        assertEquals(post.photos.originals, post.photos.details)
        assertNull(one("""{"id":"x"}""").author.avatarUrl)
    }

    @Test fun variantsPickTheFeedAndDetailFilesInFormatOrderAndAnEmptyFeedVariantStaysEmpty() = runBlocking {
        val post = one("""{"id":"v","photos":[{"url":"/o/1","variants":[
            {"purpose":"feed","contentType":"image/webp","width":10,"height":10,"sizeBytes":3,"url":"/f/webp"},
            {"purpose":"feed","contentType":"image/png","width":10,"height":10,"sizeBytes":3,"url":"/f/png"},
            {"purpose":"detail","contentType":"image/jpeg","width":10,"height":10,"sizeBytes":3,"url":"/d/jpeg"}]},
            {"url":"/o/2","variants":[{"purpose":"feed","contentType":"image/png","width":0,"height":10,"sizeBytes":3,"url":"/f/broken"}]}]}""")
        assertEquals(listOf("https://api.example/f/png", ""), post.photos.thumbnails)
        assertEquals(listOf("https://api.example/d/jpeg", "https://api.example/o/2"), post.photos.details)
    }

    @Test fun unknownStatesAndNumbersFallBackSafely() = runBlocking {
        val post = one("""{"id":"v","visibility":"Weird","moderationState":"Weird","contentRevision":"x","helpfulCount":"3","rating":{"place":"4","service":null,"coffee":5},
            "drinkSlug":"","customDrinkName":" ","drinkNameRu":null}""")
        assertEquals("Private", post.visibility.name); assertEquals("Unknown", post.moderation.name)
        assertEquals(0, post.contentRevision); assertEquals(3, post.helpfulCount)
        assertEquals(4, post.rating?.place); assertEquals(0, post.rating?.service); assertEquals(5, post.rating?.coffee)
        assertNull(post.drink)
        assertEquals("cappuccino", one("""{"id":"d","drinkSlug":"cappuccino","drinkNameRu":"Капучино"}""").drink?.slug)
    }
}
