package com.coffeepeek.feature.profile.data.repository

import com.coffeepeek.core.network.requestResult
import com.coffeepeek.feature.community.api.PublicAuthorPosts
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.data.mapper.toPublicProfileCheckIn
import com.coffeepeek.feature.profile.domain.PublicProfileCheckInPage

/** Reads only the author's public feed, including when the viewed profile belongs to the viewer. */
fun createPublicProfileCheckIns(posts: PublicAuthorPosts): PublicCheckInsPort = object : PublicCheckInsPort {
    override suspend fun page(slug: String, cursor: String?) = requestResult {
        require(slug.isNotBlank() && slug.length <= 100) { "Некорректный адрес профиля" }
        val page = posts.page(slug, cursor).getOrThrow()
        val rows = page.items.filter { it.authorSlug == slug && it.isPublicApproved }
        PublicProfileCheckInPage(rows.map { it.toPublicProfileCheckIn() }, page.nextCursor)
    }
}
