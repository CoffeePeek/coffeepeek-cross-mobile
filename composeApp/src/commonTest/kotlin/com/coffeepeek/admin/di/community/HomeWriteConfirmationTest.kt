package com.coffeepeek.admin.di.community

import com.coffeepeek.admin.di.homecheckin.homeReadFixture
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.impl.createCommunitySavedPosts
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class HomeWriteConfirmationTest {
    private class Account : CommunityAccount {
        override val accountKey = MutableStateFlow<String?>("one")
        override fun currentAccountKey() = accountKey.value
        override suspend fun ownAuthorSlug() = Result.success("me")
    }
    private class Writer : HomePreparationRepository {
        var entry = homeReadFixture()
        override suspend fun create(input: HomePreparationInput) = update(entry.id, input)
        override suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation> {
            entry = entry.copy(visibility = input.visibility, moderation = HomePreparationModeration.Pending)
            return Result.success(entry)
        }
        override suspend fun delete(id: String) = Result.success(Unit)
        override suspend fun detail(id: String) = Result.success(entry)
        override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?) =
            Result.success(HomePreparationPage(emptyList(), 0, page, pageSize))
        override suspend fun calendar(range: HomePreparationDateRange) = Result.success(emptyList<HomePreparationDay>())
    }

    @Test fun makingHomePrivateOrDeletingItClearsUnreconciledPublicConfirmation() = runTest {
        val saved = createCommunitySavedPosts()
        val writer = Writer()
        val repo = HomeCommunityWrites(writer, Account(), saved)
        val input = HomePreparationInput("2026-11-14T09:00:00Z", visibility = HomePreparationVisibility.Public)
        val post = repo.create(input).getOrThrow()
        assertEquals(post.id, saved.posts.value.single().id)
        repo.update(post.id, input.copy(visibility = HomePreparationVisibility.Private)).getOrThrow()
        assertTrue(saved.posts.value.isEmpty())
        repo.update(post.id, input).getOrThrow()
        assertEquals(post.id, saved.posts.value.single().id)
        repo.delete(post.id).getOrThrow()
        assertTrue(saved.posts.value.isEmpty())
    }
}
