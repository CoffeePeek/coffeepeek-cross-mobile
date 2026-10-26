package com.coffeepeek.feature.community.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.community.data.backend.CommunityBackend
import com.coffeepeek.feature.community.data.backend.RatingRequest
import com.coffeepeek.feature.community.data.backend.UpdateRequest
import com.coffeepeek.feature.community.data.mapper.intOrNullValue
import com.coffeepeek.feature.community.data.mapper.toDomain
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.FeedEntry
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.model.FeedPage
import com.coffeepeek.feature.community.domain.model.HelpfulVote
import com.coffeepeek.feature.community.domain.model.MyPostsPage
import com.coffeepeek.feature.community.domain.model.PostEdit
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.domain.repository.CommunityRepository
import com.coffeepeek.feature.community.domain.rules.EditIssue
import com.coffeepeek.feature.community.domain.rules.ReportIssue
import com.coffeepeek.feature.community.domain.rules.validateEdit
import com.coffeepeek.feature.community.domain.rules.validateReport
import io.ktor.client.HttpClient
import io.ktor.http.Url

/** A write was refused locally before any request was made. */
class InvalidPostEdit(val issue: EditIssue) : IllegalArgumentException(issue.name)

class InvalidReport(val issue: ReportIssue) : IllegalArgumentException(issue.name)

/** The caller supplies the authenticated client and revokes access on account replacement. */
fun createCommunityRepository(client: HttpClient, apiBaseUrl: String): CommunityRepository {
    val base = Url(apiBaseUrl)
    require(base.protocol.name in setOf("http", "https") && base.host.isNotBlank()) { "Invalid API base URL" }
    return RemoteCommunityRepository(CommunityBackend(client), apiBaseUrl)
}

private class RemoteCommunityRepository(private val backend: CommunityBackend, private val apiBaseUrl: String) : CommunityRepository {
    override suspend fun feed(pageSize: Int, cursor: String?, filters: FeedFilters): Result<FeedPage> = requestResult {
        val page = backend.feed(pageSize, cursor, filters).getOrThrow()
        FeedPage(page.items.map { FeedEntry(it.publishedAtUtc, it.checkIn.toDomain(apiBaseUrl)) }, page.nextCursor)
    }

    override suspend fun postById(id: String): Result<CommunityPost> = requestResult {
        require(id.isNotBlank()) { "Missing check-in id" }
        backend.post(id).getOrThrow().toDomain(apiBaseUrl)
    }

    override suspend fun myPosts(page: Int, pageSize: Int): Result<MyPostsPage> = requestResult {
        val number = page.coerceAtLeast(1)
        val size = pageSize.coerceIn(1, 100)
        val result = backend.mine(number, size).getOrThrow()
        val total = checkNotNull(result.totalCount.intOrNullValue()) { "Invalid total count" }
        require(total >= 0) { "Negative total count" }
        MyPostsPage(result.items.map { it.toDomain(apiBaseUrl) }, hasMore = number < pageCount(total, size))
    }

    override suspend fun update(id: String, edit: PostEdit): Result<CommunityPost> = requestResult {
        validateEdit(edit)?.let { throw InvalidPostEdit(it) }
        backend.update(id, UpdateRequest(
            text = edit.text.trim(),
            rating = edit.rating?.let { RatingRequest(it.place, it.service, it.coffee) },
            drinkSlug = edit.drinkSlug,
            customDrinkName = edit.customDrinkName?.trim(),
        )).getOrThrow().toDomain(apiBaseUrl)
    }

    override suspend fun setVisibility(id: String, visibility: PostVisibility): Result<CommunityPost> = requestResult {
        backend.setVisibility(id, visibility.name).getOrThrow().toDomain(apiBaseUrl)
    }

    override suspend fun setHelpful(id: String, helpful: Boolean): Result<HelpfulVote> = requestResult {
        val vote = backend.setHelpful(id, helpful).getOrThrow()
        HelpfulVote(vote.isHelpful, (vote.helpfulCount.intOrNullValue() ?: 0).coerceAtLeast(0))
    }

    override suspend fun report(id: String, text: String): Result<Unit> = requestResult {
        validateReport(text)?.let { throw InvalidReport(it) }
        backend.report(id, text.trim()).getOrThrow()
    }
}

private fun pageCount(total: Int, size: Int): Int = ((total.toLong() + size - 1) / size).toInt()
