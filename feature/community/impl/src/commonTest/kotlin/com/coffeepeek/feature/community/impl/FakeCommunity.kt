package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunityAnalytics
import com.coffeepeek.feature.community.api.ConsumedDrinks
import com.coffeepeek.feature.community.api.EditField
import com.coffeepeek.feature.community.api.FeedAction
import com.coffeepeek.feature.community.api.FeedItemAction
import com.coffeepeek.feature.community.api.FeedTrigger
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.DrinkOption
import com.coffeepeek.feature.community.domain.model.FeedEntry
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.model.FeedPage
import com.coffeepeek.feature.community.domain.model.HelpfulVote
import com.coffeepeek.feature.community.domain.model.MyPostsPage
import com.coffeepeek.feature.community.domain.model.PostAuthor
import com.coffeepeek.feature.community.domain.model.PostEdit
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.model.PostPhotos
import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal fun post(
    id: String = "post", slug: String? = "other", authorId: String? = "author-$id", note: String = "", home: Boolean = false,
) = CommunityPost(id, "shop", "Shop", PostAuthor(authorId, slug, "Name", null), note, "2026-10-07T10:00:00Z", "2026-10-07T09:00:00Z", PostPhotos.None,
    PostRating(4, 4, 5), null, PostVisibility.Public, PostModeration.Approved, 0, null, 0, false, home, null)

internal fun entry(post: CommunityPost) = FeedEntry("2026-10-07T10:00:00Z", post)

internal class FakeRepository : CommunityRepository {
    var feed: suspend (Int, String?, FeedFilters) -> Result<FeedPage> = { _, _, _ -> Result.success(FeedPage(emptyList(), null)) }
    var mine: suspend (Int, Int) -> Result<MyPostsPage> = { _, _ -> Result.success(MyPostsPage(emptyList(), false)) }
    var helpful: suspend (String, Boolean) -> Result<HelpfulVote> = { _, _ -> error("Unused") }
    var update: suspend (String, PostEdit) -> Result<CommunityPost> = { _, _ -> error("Unused") }
    var visibility: suspend (String, PostVisibility) -> Result<CommunityPost> = { _, _ -> error("Unused") }
    var report: suspend (String, String) -> Result<Unit> = { _, _ -> error("Unused") }
    val feedRequests = mutableListOf<Triple<Int, String?, FeedFilters>>()
    override suspend fun feed(pageSize: Int, cursor: String?, filters: FeedFilters): Result<FeedPage> {
        feedRequests += Triple(pageSize, cursor, filters)
        return feed.invoke(pageSize, cursor, filters)
    }
    override suspend fun myPosts(page: Int, pageSize: Int) = mine(page, pageSize)
    override suspend fun setHelpful(id: String, helpful: Boolean) = this.helpful(id, helpful)
    override suspend fun update(id: String, edit: PostEdit) = update.invoke(id, edit)
    override suspend fun setVisibility(id: String, visibility: PostVisibility) = this.visibility(id, visibility)
    override suspend fun report(id: String, text: String) = report.invoke(id, text)
}

internal class FakeAccount(initial: String? = null) : CommunityAccount {
    private val key = MutableStateFlow(initial)
    var ownSlug: suspend () -> Result<String?> = { Result.success("me") }
    override val accountKey = key
    override fun currentAccountKey(): String? = key.value
    override suspend fun ownAuthorSlug() = ownSlug()
    fun switchTo(account: String?) { key.value = account }
}

internal class FakeAnalytics : CommunityAnalytics {
    val events = mutableListOf<String>()
    override fun feedAction(action: FeedAction, trigger: FeedTrigger) { events += "feed:$action:$trigger" }
    override fun itemInteracted(action: FeedItemAction, isHome: Boolean) { events += "item:$action:${if (isHome) "home" else "shop"}" }
    override fun likeGateShown() { events += "gate:like" }
    override fun editOpened() { events += "edit:opened" }
    override fun editFieldChanged(field: EditField) { events += "edit:field:$field" }
    override fun editSubmitted() { events += "edit:submitted" }
    override fun editRejected() { events += "edit:rejected" }
    override fun editSaved(visibility: PostVisibility, hasPhotos: Boolean) { events += "edit:saved:$visibility:$hasPhotos" }
    override fun editFailed(error: Throwable) { events += "edit:failed" }
    override fun editCancelled() { events += "edit:cancelled" }
}

internal class FakeAuthors(ready: Boolean = true) : AuthorVisibility {
    override val visibility = MutableStateFlow(AuthorVisibilitySnapshot(ready = ready))
    var refreshes = 0
    var refresh: suspend () -> Result<Unit> = { visibility.value = AuthorVisibilitySnapshot(revision = visibility.value.revision + 1); Result.success(Unit) }
    override suspend fun refresh(): Result<Unit> { refreshes++; return refresh.invoke() }
}

internal class FakeDrinks : ConsumedDrinks {
    var load: suspend () -> Result<List<DrinkOption>> = { Result.success(listOf(DrinkOption("cappuccino", "Капучино", "Cappuccino"), DrinkOption("other", "Другое", "Other"))) }
    var loads = 0
    override suspend fun load(): Result<List<DrinkOption>> { loads++; return load.invoke() }
}
