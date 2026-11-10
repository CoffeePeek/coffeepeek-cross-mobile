package com.coffeepeek.feature.community.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.community.api.PublicAuthorPosts
import com.coffeepeek.feature.community.api.PublicAuthorPostPage
import com.coffeepeek.feature.community.api.PublicAuthorPost
import com.coffeepeek.feature.community.api.PublicPostScores
import com.coffeepeek.feature.community.api.PublicPostDrink
import com.coffeepeek.feature.community.api.PublicPostPreparation
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.repository.CommunityRepository

/** Shares the existing feed transport and mapping without exposing writes to another feature. */
fun createPublicAuthorPosts(repository: CommunityRepository): PublicAuthorPosts = object : PublicAuthorPosts {
    override suspend fun page(slug: String, cursor: String?) = requestResult {
        require(slug.isNotBlank() && slug.length <= 100) { "Invalid author slug" }
        val page = repository.feed(20, cursor, FeedFilters(authorSlug = slug)).getOrThrow()
        PublicAuthorPostPage(page.entries.map { it.post.publicRead() }, page.nextCursor)
    }
}

private fun CommunityPost.publicRead() = PublicAuthorPost(
    id = id, authorSlug = author.slug, authorName = author.name, authorAvatarUrl = author.avatarUrl,
    shopSlug = shopSlug, shopName = shopName, note = note, createdAt = createdAt, visitedAt = visitedAt,
    isPublicApproved = visibility == PostVisibility.Public && moderation == PostModeration.Approved,
    isHome = isHome, scores = rating?.let { PublicPostScores(it.place, it.service, it.coffee) },
    drink = drink?.let { PublicPostDrink(it.customName, it.nameRu, it.nameEn) },
    home = home?.let {
        PublicPostPreparation(it.preparedOn, it.brewMethodCode, it.beanName, it.doseGrams, it.brewDurationSeconds, it.tasteScore)
    }, originals = photos.originals, thumbnails = photos.thumbnails, details = photos.details,
    helpfulCount = helpfulCount, isHelpfulByCurrentUser = isHelpfulByCurrentUser,
)
