package com.coffeepeek.feature.community.domain.model

enum class PostVisibility { Private, Public }

enum class PostModeration { NotSubmitted, Pending, Approved, Rejected, Unknown }

data class PostRating(val place: Int, val service: Int, val coffee: Int)

/** [id] is the opaque public identity (author blocking); [slug] is the canonical profile slug. */
data class PostAuthor(val id: String?, val slug: String?, val name: String, val avatarUrl: String?)

data class PostDrink(val slug: String?, val customName: String?, val nameRu: String?, val nameEn: String?)

data class HomePreparation(
    val preparedOn: String,
    val brewMethodCode: String?,
    val beanName: String?,
    val doseGrams: Double?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
)

/** [thumbnails] may hold an empty string for a photo that has no usable feed variant; [details] falls back to the original. */
data class PostPhotos(val originals: List<String>, val thumbnails: List<String>, val details: List<String>) {
    companion object { val None = PostPhotos(emptyList(), emptyList(), emptyList()) }
}

/** One check-in as the timeline shows it, for a shop visit ([home] is null) or a home preparation. */
data class CommunityPost(
    val id: String,
    val shopSlug: String,
    val shopName: String,
    val author: PostAuthor,
    val note: String,
    val createdAt: String,
    val visitedAt: String,
    val photos: PostPhotos,
    val rating: PostRating?,
    val drink: PostDrink?,
    val visibility: PostVisibility,
    val moderation: PostModeration,
    val contentRevision: Int,
    val rejectionReason: String?,
    val helpfulCount: Int,
    val isHelpfulByCurrentUser: Boolean,
    val isHome: Boolean,
    val home: HomePreparation?,
)
