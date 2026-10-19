package com.coffeepeek.admin.feature.authorblocking

import com.coffeepeek.feature.authorblocking.api.*
import com.coffeepeek.admin.feature.community.ui.*
import com.coffeepeek.domain.feature.feed.*
import com.coffeepeek.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.test.*

class AuthorVisibilityFeedTest {
    @Test fun blockInvalidatesCursorAndLatePageCannotRestoreAuthor_UnblockReloads(): Unit = runBlocking {
        val author = CheckIn("post", "shop", "Shop", "Note", "2026-10-01", authorId = "author",
            authorAddress = PublicAddress("old-slug", "/users/old-slug", 1, false))
        val second = author.copy(id = "visible", authorId = "other", authorAddress = null)
        val visibility = TestAuthorVisibility()
        val pageStarted = CompletableDeferred<Job>(); val release = CompletableDeferred<Unit>()
        val feed = TestPublicFeed().apply { load = { _, cursor, _ ->
            if (cursor != null) withContext(NonCancellable) {
                pageStarted.complete(currentCoroutineContext().job); release.await()
                Result.success(FeedPage(listOf(FeedItem("2026-10-01", author)), null))
            } else Result.success(FeedPage(listOf(FeedItem("2026-10-01", author), FeedItem("2026-10-01", second)), "next"))
        } }
        val vm = CommunityViewModel(FeedCheckIns(), FeedSessions(Session("token", userId = "viewer")), FeedShops(), feed, FeedUsers(), visibility)
        try {
            withTimeout(5_000) { vm.state.first { it.items.size == 2 && !it.isLoading } }
            vm.loadMore(); pageStarted.await()
            visibility.visibility.value = AuthorVisibilitySnapshot(1, blockedIds = setOf("author"))
            withTimeout(5_000) { vm.state.first { it.items.map(CheckIn::id) == listOf("visible") && !it.isLoading } }
            release.complete(Unit)
            withTimeout(5_000) { vm.state.first { !it.isLoadingMore } }
            assertEquals(listOf("visible"), vm.state.value.items.map(CheckIn::id))
            visibility.visibility.value = AuthorVisibilitySnapshot(2)
            withTimeout(5_000) { vm.state.first { it.items.size == 2 && !it.isLoading } }
            assertEquals(1, vm.state.value.page)
        } finally { release.complete(Unit); vm.close() }
    }
}

private class TestAuthorVisibility : AuthorVisibility {
    override val visibility = MutableStateFlow(AuthorVisibilitySnapshot())
    override suspend fun refresh() = Result.success(Unit)
}
