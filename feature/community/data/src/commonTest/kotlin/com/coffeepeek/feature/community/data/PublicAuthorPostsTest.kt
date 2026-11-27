package com.coffeepeek.feature.community.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.community.api.PublicAuthorPostPage
import com.coffeepeek.feature.community.data.repository.createCommunityRepository
import com.coffeepeek.feature.community.data.repository.createPublicAuthorPosts
import com.coffeepeek.feature.community.domain.model.FeedLoadException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublicAuthorPostsTest {
    @Test fun authorReadUsesSharedTransportAndPreservesRichContentAndVariantSemantics() = runTest {
        val client = HttpClientFactory(MockEngine { request ->
            assertEquals("/api/v1/feed", request.url.encodedPath)
            assertEquals("anna", request.url.parameters["authorSlug"])
            assertEquals("20", request.url.parameters["pageSize"])
            assertEquals("cursor", request.url.parameters["cursor"])
            assertEquals("no-store", request.headers[HttpHeaders.CacheControl])
            assertEquals(setOf("authorSlug", "cursor", "pageSize"), request.url.parameters.names())
            respond(richFeed, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }).api("https://api.example")
        try {
            val page = createPublicAuthorPosts(createCommunityRepository(client, "https://api.example/"))
                .page("anna", "cursor").getOrThrow()
            assertPublicRead(page)
        } finally { client.close() }
    }

    private fun assertPublicRead(page: PublicAuthorPostPage) {
        assertEquals("next", page.nextCursor)
        assertEquals(listOf("shop", "home", "private", "unknown", "without-bag"), page.items.map { it.id })
        val shop = page.items.first()
        assertTrue(shop.isPublicApproved)
        assertEquals("https://api.example/avatar", shop.authorAvatarUrl)
        assertEquals(listOf("https://api.example/photo"), shop.originals)
        assertEquals(listOf(""), shop.thumbnails)
        assertEquals(listOf("https://api.example/photo"), shop.details)
        assertEquals(4, shop.scores?.service)
        assertEquals("Special", shop.drink?.customName)
        assertEquals(3, shop.helpfulCount)
        assertTrue(shop.isHelpfulByCurrentUser)
        assertHomeRead(page)
        assertFalse(page.items[2].isPublicApproved)
        assertFalse(page.items[3].isPublicApproved)
    }

    private fun assertHomeRead(page: PublicAuthorPostPage) {
        val home = page.items[1]
        assertTrue(home.isHome)
        assertEquals("prepared", home.home?.preparedOn)
        assertEquals("Ethiopia", home.home?.beanName)
        assertEquals("Roaster", home.home?.roasterName)
        assertEquals("ethiopia", home.home?.coffeeProductSlug)
        assertEquals("https://api.example/bag/thumb", home.home?.shelfBagCardUrl)
        assertTrue(home.originals.isEmpty())
        val withoutBag = page.items.last().home
        assertEquals("Personal beans", withoutBag?.beanName)
        assertNull(withoutBag?.shelfBagCardUrl)
        assertNull(withoutBag?.roasterName)
        assertNull(withoutBag?.coffeeProductSlug)
    }

    @Test fun invalidAddressFailsWithoutRequestsAndCursorFailureKeepsRestartSignal() = runTest {
        var calls = 0
        val client = HttpClientFactory(MockEngine { calls++; respond("", HttpStatusCode.BadRequest) }).api("https://api.example")
        try {
            val reader = createPublicAuthorPosts(createCommunityRepository(client, "https://api.example"))
            for (slug in listOf("", " ", "x".repeat(101))) assertTrue(reader.page(slug).isFailure)
            assertEquals(0, calls)
            assertTrue(assertIs<FeedLoadException>(reader.page("anna", "expired").exceptionOrNull()).restartPagination)
            assertEquals(1, calls)
        } finally { client.close() }
    }

    @Test fun cancellationIsNeverReturnedAsAnOrdinaryFailure() = runTest {
        val client = HttpClientFactory(MockEngine { throw CancellationException("cancelled") }).api("https://api.example")
        try {
            val reader = createPublicAuthorPosts(createCommunityRepository(client, "https://api.example"))
            assertFailsWith<CancellationException> { reader.page("anna") }
        } finally { client.close() }
    }

    private val richFeed = """{"isSuccess":true,"data":{"items":[
        {"checkIn":{"id":"shop","author":{"slug":"anna"},"authorAvatarUrl":"/avatar","shop":{"slug":"cafe"},
        "visibility":"Public","moderationState":"Approved","createdAtUtc":"created","visitedAt":"visited",
        "rating":{"place":3,"service":4,"coffee":5},"customDrinkName":"Special","helpfulCount":"3","isHelpfulByCurrentUser":true,
        "photos":[{"url":"/photo","variants":[]}]}},
        {"checkIn":{"id":"home","author":{"slug":"anna"},"kind":"Home","visibility":"Public","moderationState":"Approved",
        "home":{"preparedOn":"prepared","beanName":"Ethiopia","doseGrams":18,"tasteScore":5,
        "roasterName":"Roaster","coffeeProductSlug":"ethiopia"},"shelfBagPhotoUrl":"/bag",
        "shelfBagPhotoVariants":[{"purpose":"thumb","contentType":"image/jpeg","width":240,"height":320,"sizeBytes":9,"url":"/bag/thumb"}]}},
        {"checkIn":{"id":"private","visibility":"Private","moderationState":"Approved"}},
        {"checkIn":{"id":"unknown","visibility":"Public","moderationState":"FutureValue"}},
        {"checkIn":{"id":"without-bag","kind":"Home","home":{"preparedOn":"prepared","beanName":"Personal beans"}}}
        ],"nextCursor":"next"}}"""
}
