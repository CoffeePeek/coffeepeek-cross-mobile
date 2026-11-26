package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.di.community.CommunityProfileAccounts
import com.coffeepeek.core.network.HttpClientFactory
import com.coffeepeek.feature.community.api.CommunitySavedPosts
import com.coffeepeek.feature.community.data.repository.createCommunityRepository
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileCheckInLikes
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.domain.PublicProfileAuthenticationRequired
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProfileCheckInLikesTest {
    private class Account : ProfileAccountPort by CommunityProfileAccounts() {
        var user: String? = "viewer"
        var scope = "1:viewer"
        override fun activeAccount() = user
        override fun cacheScope() = scope.takeIf { user != null }
    }

    private class Saved : CommunitySavedPosts by CommunitySavedPosts.None {
        val updatedAccounts = mutableListOf<String>()
        override fun updated(accountKey: String) { updatedAccounts.add(accountKey) }
    }

    private class Fixture {
        val account = Account()
        val saved = Saved()
        var count = 3
        var selected = false
        var mutations = 0
        var status = HttpStatusCode.OK
        var afterVote: () -> Unit = {}
        private val client = HttpClientFactory(MockEngine { request ->
            val headers = headersOf(HttpHeaders.ContentType, "application/json")
            val body = if (request.method == HttpMethod.Get) feed() else {
                assertEquals("/api/v1/check-ins/post/helpful", request.url.encodedPath)
                mutations++
                selected = request.method == HttpMethod.Put
                count += if (selected) 1 else -1
                afterVote()
                """{"isSuccess":true,"data":{"isHelpful":$selected,"helpfulCount":$count}}"""
            }
            respond(body, status, headers)
        }).api("https://fixture.invalid")
        private val repository = createCommunityRepository(client, "https://fixture.invalid")
        private val graph = koinApplication {
            modules(profileCheckInsModule(), module {
                single<CommunityRepository> { repository }
                single<ProfileAccountPort> { account }
                single<CommunitySavedPosts> { saved }
            })
        }
        val likes = graph.koin.get<ProfileCheckInLikes>()
        suspend fun row() = graph.koin.get<PublicCheckInsPort>().page("author").getOrThrow().items.single()
        fun close() {
            graph.close()
            client.close()
        }
        private fun feed() = """{"isSuccess":true,"data":{"items":[{"checkIn":{
            "id":"post","author":{"slug":"author"},"visibility":"Public","moderationState":"Approved",
            "helpfulCount":$count,"isHelpfulByCurrentUser":$selected
        }}]}}"""
    }

    @Test fun theProductionGraphReadsLikesAndWritesThroughTheSharedCommunityTransport() = runTest {
        val fixture = Fixture()
        try {
            assertEquals(3, fixture.row().helpfulCount)
            fixture.likes.setHelpful("post", true).getOrThrow()
            assertEquals(4, fixture.row().helpfulCount)
            assertTrue(fixture.row().isHelpfulByCurrentUser)
            fixture.likes.setHelpful("post", false).getOrThrow()
            assertEquals(3, fixture.row().helpfulCount)
            assertEquals(listOf("viewer", "viewer"), fixture.saved.updatedAccounts)
        } finally { fixture.close() }
    }

    @Test fun guestsNeverSendAWriteAndUnauthorizedResponsesRequireSignIn() = runTest {
        val fixture = Fixture()
        try {
            fixture.account.user = null
            assertIs<PublicProfileAuthenticationRequired>(fixture.likes.setHelpful("post", true).exceptionOrNull())
            assertEquals(0, fixture.mutations)
            fixture.account.user = "viewer"
            fixture.status = HttpStatusCode.Unauthorized
            assertIs<PublicProfileAuthenticationRequired>(fixture.likes.setHelpful("post", true).exceptionOrNull())
            assertTrue(fixture.saved.updatedAccounts.isEmpty())
        } finally { fixture.close() }
    }

    @Test fun responsesFromAReplacedSessionNeverInvalidateTheNewAccountsFeed() = runTest {
        val fixture = Fixture()
        try {
            fixture.afterVote = { fixture.account.scope = "2:viewer" }
            assertFailsWith<CancellationException> { fixture.likes.setHelpful("post", true) }
            assertTrue(fixture.saved.updatedAccounts.isEmpty())
        } finally { fixture.close() }
    }
}
