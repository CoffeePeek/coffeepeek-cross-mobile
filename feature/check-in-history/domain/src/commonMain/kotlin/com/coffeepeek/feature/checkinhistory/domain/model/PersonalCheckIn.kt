package com.coffeepeek.feature.checkinhistory.domain.model

/** Owner history, including private or moderated entries; not a public feed card. */
data class PersonalCheckIn(
    val id: String,
    val shop: HistoryAddress?,
    val shopName: String,
    val author: HistoryAddress?,
    val username: String,
    val note: String,
    val createdAt: String,
    val visitedAt: String,
    val rating: HistoryRating?,
    val photoUrls: List<String>,
    val drinkPhotoUrl: String?,
    val drink: HistoryDrink?,
    val visibility: HistoryVisibility,
    val moderation: HistoryModeration,
    val contentRevision: Int,
    val rejectionReason: String?,
    val helpfulCount: Int,
    val isHelpfulByCurrentUser: Boolean,
) {
    val effectiveVisitedAt: String get() = visitedAt.ifBlank { createdAt }
}

data class HistoryAddress(val slug: String, val canonicalPath: String)

data class HistoryRating(val place: Int?, val service: Int?, val coffee: Int?)

data class HistoryDrink(
    val slug: String?,
    val customName: String?,
    val nameRu: String?,
    val nameEn: String?,
)

enum class HistoryVisibility { Private, Public }

enum class HistoryModeration { NotSubmitted, Pending, Approved, Rejected, Unknown }
