package com.coffeepeek.feature.community.data

import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.community.data.repository.InvalidPostEdit
import com.coffeepeek.feature.community.data.repository.InvalidReport
import com.coffeepeek.feature.community.data.repository.createCommunityRepository
import com.coffeepeek.feature.community.domain.model.CommunityFailure
import com.coffeepeek.feature.community.domain.model.FailureKind
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.model.FeedLoadException
import com.coffeepeek.feature.community.domain.model.PostEdit
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.domain.rules.EditIssue
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommunityRepositoryTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private fun repository(engine: MockEngine) = createCommunityRepository(HttpClientFactory(engine).api("https://api.example"), "https://api.example/")

    @Test fun feedSendsCursorFiltersAndNoStoreAndKeepsThePublicationOrder() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/v1/feed", request.url.encodedPath)
            assertEquals("20", request.url.parameters["pageSize"]); assertEquals("cur", request.url.parameters["cursor"])
            assertEquals("minsk", request.url.parameters["citySlug"]); assertEquals("cafe", request.url.parameters["coffeeShopSlug"]); assertEquals("anna", request.url.parameters["authorSlug"])
            assertEquals("no-store", request.headers[HttpHeaders.CacheControl])
            respond("""{"isSuccess":true,"data":{"items":[
                {"publishedAtUtc":"2026-10-07T10:00:00Z","checkIn":{"id":"old-visit","visitedAt":"2019-01-01T00:00:00Z","shop":{"slug":"cafe"},"author":{"slug":"anna"},"authorId":"a-1","username":"Anna","text":"First","rating":{"coffee":5,"service":4,"place":4},"visibility":"Public","moderationState":"Approved"}},
                {"publishedAtUtc":"2026-10-07T09:00:00Z","checkIn":{"id":"new-visit","createdAtUtc":"2026-10-07T08:00:00Z","note":"Second"}}
            ],"nextCursor":"next"}}""", headers = json)
        }
        val page = repository(engine).feed(20, "cur", FeedFilters("minsk", "cafe", "anna")).getOrThrow()
        assertEquals(listOf("old-visit", "new-visit"), page.entries.map { it.post.id })
        assertEquals("2026-10-07T10:00:00Z", page.entries.first().publishedAtUtc)
        assertEquals("next", page.nextCursor)
        val first = page.entries.first().post
        assertEquals("anna", first.author.slug); assertEquals("a-1", first.author.id); assertEquals("cafe", first.shopSlug)
        assertEquals(PostRating(4, 4, 5), first.rating); assertEquals(PostVisibility.Public, first.visibility); assertEquals(PostModeration.Approved, first.moderation)
        assertEquals("Second", page.entries.last().post.note) // the legacy "note" alias
        assertEquals(PostVisibility.Private, page.entries.last().post.visibility)
    }

    @Test fun invalidCursorRequiresARestartButAnUnknownFilterDoesNot() = runBlocking {
        for (status in listOf(HttpStatusCode.BadRequest, HttpStatusCode.NotFound, HttpStatusCode.InternalServerError)) {
            var requests = 0
            val error = repository(MockEngine { requests++; respond("", status) }).feed(20, "old", FeedFilters(citySlug = "unknown")).exceptionOrNull()
            assertEquals(status == HttpStatusCode.BadRequest, assertIs<FeedLoadException>(error).restartPagination)
            assertEquals(1, requests)
        }
        // Without a cursor a 400 starts nothing to restart.
        val first = repository(MockEngine { respond("", HttpStatusCode.BadRequest) }).feed(20).exceptionOrNull()
        assertEquals(false, assertIs<FeedLoadException>(first).restartPagination)
    }

    @Test fun feedRejectsAnOutOfRangePageSizeAndBlankFiltersBeforeAnyRequest() = runBlocking {
        var requests = 0
        val repo = repository(MockEngine { requests++; respond("""{"isSuccess":true,"data":{"items":[]}}""", headers = json) })
        assertTrue(repo.feed(0).isFailure); assertTrue(repo.feed(101).isFailure)
        assertTrue(repo.feed(20, filters = FeedFilters(citySlug = "")).isFailure)
        assertTrue(repo.feed(20, filters = FeedFilters(authorSlug = "x".repeat(101))).isFailure)
        assertEquals(0, requests)
        assertTrue(repo.feed(100).isSuccess)
    }

    @Test fun anEnvelopeFailureAndMissingDataNeverBecomeASuccessfulFeed() = runBlocking {
        assertTrue(repository(MockEngine { respond("""{"isSuccess":false,"message":"no"}""", headers = json) }).feed(20).isFailure)
        assertTrue(repository(MockEngine { respond("""{"isSuccess":true}""", headers = json) }).feed(20).isFailure)
        assertTrue(repository(MockEngine { respond("""{"isSuccess":true,"data":{"items":[{"publishedAtUtc":"x","checkIn":{"id":""}}]}}""", headers = json) }).feed(20).isFailure)
    }

    @Test fun myPostsUseTheMineRouteAndHasMoreComesFromTheTotalCount() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/check-ins/mine", request.url.encodedPath)
            assertEquals("2", request.url.parameters["pageNumber"]); assertEquals("20", request.url.parameters["pageSize"])
            respond("""{"isSuccess":true,"data":{"totalCount":"41","items":[{"id":"mine","text":"Private","visibility":"Private","moderationState":"Rejected","rejectionReason":"Reason"}]}}""", headers = json)
        }
        val page = repository(engine).myPosts(2, 20).getOrThrow()
        assertEquals("mine", page.posts.single().id); assertEquals(PostModeration.Rejected, page.posts.single().moderation); assertEquals("Reason", page.posts.single().rejectionReason)
        assertTrue(page.hasMore) // 41 posts: page 2 of 3
        val last = repository(MockEngine { respond("""{"isSuccess":true,"data":{"totalCount":40,"items":[]}}""", headers = json) }).myPosts(2, 20).getOrThrow()
        assertEquals(false, last.hasMore)
        assertTrue(repository(MockEngine { respond("""{"isSuccess":true,"data":{"items":[]}}""", headers = json) }).myPosts(1, 20).isFailure)
        assertTrue(repository(MockEngine { respond("""{"isSuccess":true,"data":{"totalCount":-1,"items":[]}}""", headers = json) }).myPosts(1, 20).isFailure)
    }

    @Test fun updateSendsTrimmedValuesAndReturnsTheAuthoritativePostAndInvalidEditsSendNothing() = runBlocking {
        var body = ""
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Put, request.method); assertEquals("/api/v1/check-ins/own", request.url.encodedPath)
            body = request.body.toByteArray().decodeToString()
            respond("""{"isSuccess":true,"data":{"id":"own","text":"Edited","visibility":"Public","moderationState":"Pending","contentRevision":"2"}}""", headers = json)
        }
        val repo = repository(engine)
        val updated = repo.update("own", PostEdit("  Edited ", PostRating(4, 5, 3), "other", " Custom ")).getOrThrow()
        assertEquals(PostModeration.Pending, updated.moderation); assertEquals(2, updated.contentRevision)
        assertTrue("\"text\":\"Edited\"" in body && "\"customDrinkName\":\"Custom\"" in body && "\"place\":4" in body && "\"coffee\":3" in body, body)
        body = ""
        val error = repo.update("own", PostEdit("x".repeat(1001))).exceptionOrNull()
        assertEquals(EditIssue.NoteTooLong, assertIs<InvalidPostEdit>(error).issue); assertEquals("", body)
    }

    @Test fun visibilityUsesItsOwnRouteAndBody() = runBlocking {
        var body = ""
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Put, request.method); assertEquals("/api/v1/check-ins/own/visibility", request.url.encodedPath)
            body = request.body.toByteArray().decodeToString()
            respond("""{"isSuccess":true,"data":{"id":"own","visibility":"Private"}}""", headers = json)
        }
        assertEquals(PostVisibility.Private, repository(engine).setVisibility("own", PostVisibility.Private).getOrThrow().visibility)
        assertEquals("""{"visibility":"Private"}""", body)
    }

    @Test fun helpfulUsesPutOrDeleteWithoutABodyAndReturnsTheAuthoritativeCount() = runBlocking {
        val methods = mutableListOf<HttpMethod>()
        val engine = MockEngine { request ->
            methods += request.method; assertEquals("/api/v1/check-ins/post/helpful", request.url.encodedPath)
            assertEquals(0, request.body.toByteArray().size)
            respond("""{"isSuccess":true,"data":{"isHelpful":${request.method == HttpMethod.Put},"helpfulCount":"8"}}""", headers = json)
        }
        val repo = repository(engine)
        assertEquals(8, repo.setHelpful("post", true).getOrThrow().helpfulCount)
        assertEquals(false, repo.setHelpful("post", false).getOrThrow().isHelpful)
        assertEquals(listOf(HttpMethod.Put, HttpMethod.Delete), methods)
    }

    @Test fun reportPostsTheTrimmedTextAndNeedsA201WithData() = runBlocking {
        var body = ""
        val ok = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method); assertEquals("/api/v1/check-ins/post/reports", request.url.encodedPath)
            body = request.body.toByteArray().decodeToString()
            respond("""{"isSuccess":true,"data":{"id":"r"}}""", HttpStatusCode.Created, json)
        }
        assertTrue(repository(ok).report("post", "  Reason ").isSuccess); assertEquals("""{"text":"Reason"}""", body)
        assertTrue(repository(MockEngine { respond("""{"isSuccess":true,"data":{"id":"r"}}""", HttpStatusCode.OK, json) }).report("post", "Reason").isFailure)
        assertTrue(repository(MockEngine { respond("""{"isSuccess":true}""", HttpStatusCode.Created, json) }).report("post", "Reason").isFailure)
        var requests = 0
        val repo = repository(MockEngine { requests++; respond("", HttpStatusCode.Created) })
        assertIs<InvalidReport>(repo.report("post", "  ").exceptionOrNull()); assertIs<InvalidReport>(repo.report("post", "x".repeat(2001)).exceptionOrNull())
        assertEquals(0, requests)
    }

    @Test fun refusedMutationsBecomeTypedFailuresWithTheServerMessageWhenThereIsOne() = runBlocking {
        val cases = mapOf(HttpStatusCode.Unauthorized to FailureKind.SignInRequired, HttpStatusCode.Forbidden to FailureKind.Forbidden,
            HttpStatusCode.NotFound to FailureKind.NotFound, HttpStatusCode.InternalServerError to FailureKind.Other)
        for ((status, kind) in cases) {
            val bare = repository(MockEngine { respond("<html>", status) }).setHelpful("post", true).exceptionOrNull()
            assertEquals(kind, assertIs<CommunityFailure>(bare).kind); assertNull(bare.serverMessage)
            val told = repository(MockEngine { respond("""{"isSuccess":false,"message":"Server says no"}""", status, json) }).report("post", "Reason").exceptionOrNull()
            assertEquals("Server says no", assertIs<CommunityFailure>(told).serverMessage)
        }
        assertEquals("Nope", assertIs<CommunityFailure>(repository(MockEngine { respond("""{"isSuccess":false,"message":"Nope"}""", headers = json) }).setVisibility("p", PostVisibility.Public).exceptionOrNull()).serverMessage)
    }

    @Test fun cancellationIsRethrownAndAConnectionFailureIsReturned() = runBlocking {
        val cancellation = CancellationException("cancelled")
        assertFailsWith<CancellationException> { repository(MockEngine { throw cancellation }).feed(20) }
        assertTrue(repository(MockEngine { throw IllegalStateException("offline") }).setHelpful("post", true).isFailure)
    }
}
