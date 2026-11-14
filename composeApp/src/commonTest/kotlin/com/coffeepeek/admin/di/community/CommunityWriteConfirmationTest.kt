package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.impl.createCommunitySavedPosts
import com.coffeepeek.feature.shop.domain.model.*
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CommunityWriteConfirmationTest {
    private class Account : CommunityAccount {
        override val accountKey = MutableStateFlow<String?>("one")
        override fun currentAccountKey() = accountKey.value
        override suspend fun ownAuthorSlug() = Result.success("me")
    }
    private class Writer : ShopCheckInRepository {
        var write: suspend () -> Result<String> = { Result.success("created") }
        override suspend fun getDrinkOptions() = Result.success(emptyList<ShopConsumedDrinkOption>())
        override suspend fun create(input: ShopCheckInCreateInput) = write()
    }
    private val input = ShopCheckInCreateInput("shop", "Note", ShopRating(4, 4, 4), "2026-11-14T09:00:00Z")

    @Test fun onlyConfirmedPublicSavesProduceAFeedConfirmation() = runTest {
        val saved = createCommunitySavedPosts()
        val writer = Writer()
        val repo = ShopCommunityWrites(writer, Account(), saved)
        repo.create(input.copy(visibility = ShopCheckInVisibility.Private)).getOrThrow()
        assertTrue(saved.posts.value.isEmpty())
        writer.write = { Result.failure(Exception("timeout")) }
        assertTrue(repo.create(input).isFailure)
        assertTrue(saved.posts.value.isEmpty())
        writer.write = { Result.success("created") }
        repo.create(input).getOrThrow()
        assertEquals("created", saved.posts.value.single().id)
    }

    @Test fun oldAccountAndCancelledWritesNeverPublishConfirmations() = runTest {
        val saved = createCommunitySavedPosts()
        val account = Account()
        val writer = Writer().apply { write = { account.accountKey.value = "two"; Result.success("old") } }
        val repo = ShopCommunityWrites(writer, account, saved)
        repo.create(input).getOrThrow()
        assertTrue(saved.posts.value.isEmpty())
        writer.write = { throw CancellationException("cancelled") }
        assertFailsWith<CancellationException> { repo.create(input) }
        assertTrue(saved.posts.value.isEmpty())
    }
}
