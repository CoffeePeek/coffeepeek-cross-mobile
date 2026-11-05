package com.coffeepeek.feature.community.api

/** Read-only author feed in publication order. Consumers still enforce their own visibility rules. */
interface PublicAuthorPosts {
    suspend fun page(slug: String, cursor: String? = null): Result<PublicAuthorPostPage>
}

data class PublicAuthorPostPage(val items: List<PublicAuthorPost>, val nextCursor: String?)

/** Deliberate read projection; no domain implementation types or mutation capabilities cross this boundary. */
data class PublicAuthorPost(
    val id: String,
    val authorSlug: String?,
    val authorName: String,
    val authorAvatarUrl: String?,
    val shopSlug: String,
    val shopName: String,
    val note: String,
    val createdAt: String,
    val visitedAt: String,
    val isPublicApproved: Boolean,
    val isHome: Boolean,
    val scores: PublicPostScores?,
    val drink: PublicPostDrink?,
    val home: PublicPostPreparation?,
    val originals: List<String>,
    val thumbnails: List<String>,
    val details: List<String>,
    val helpfulCount: Int,
    val isHelpfulByCurrentUser: Boolean,
)

data class PublicPostScores(val place: Int, val service: Int, val coffee: Int)

data class PublicPostDrink(val customName: String?, val nameRu: String?, val nameEn: String?)

data class PublicPostPreparation(
    val preparedOn: String,
    val brewMethodCode: String?,
    val beanName: String?,
    val doseGrams: Double?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
)
