package com.coffeepeek.feature.community.domain.repository

import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.model.FeedPage
import com.coffeepeek.feature.community.domain.model.HelpfulVote
import com.coffeepeek.feature.community.domain.model.MyPostsPage
import com.coffeepeek.feature.community.domain.model.PostEdit
import com.coffeepeek.feature.community.domain.model.PostVisibility

interface CommunityRepository {
    /** The public timeline in the server's publication order. Failures are [com.coffeepeek.feature.community.domain.model.FeedLoadException]. */
    suspend fun feed(pageSize: Int, cursor: String? = null, filters: FeedFilters = FeedFilters()): Result<FeedPage>

    /** The signed-in user's own posts, newest first. */
    suspend fun myPosts(page: Int, pageSize: Int): Result<MyPostsPage>

    suspend fun update(id: String, edit: PostEdit): Result<CommunityPost>
    suspend fun setVisibility(id: String, visibility: PostVisibility): Result<CommunityPost>
    suspend fun setHelpful(id: String, helpful: Boolean): Result<HelpfulVote>
    suspend fun report(id: String, text: String): Result<Unit>
}
