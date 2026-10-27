package com.coffeepeek.feature.community.domain.rules

import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.PostEdit
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.domain.model.PostVisibility

const val MAX_NOTE_LENGTH = 1000
const val MAX_REPORT_LENGTH = 2000
const val MAX_DRINK_NAME_LENGTH = 100
const val OTHER_DRINK = "other"

enum class EditIssue { NoteTooLong, RatingOutOfRange, DrinkNameRequired, DrinkNameNotAllowed }

/** The order the legacy form checked: note, rating, drink. */
fun validateEdit(edit: PostEdit): EditIssue? = when {
    edit.text.trim().length > MAX_NOTE_LENGTH -> EditIssue.NoteTooLong
    edit.rating?.let(::outOfRange) == true -> EditIssue.RatingOutOfRange
    edit.drinkSlug == OTHER_DRINK && edit.customDrinkName.orEmpty().trim().length !in 1..MAX_DRINK_NAME_LENGTH -> EditIssue.DrinkNameRequired
    edit.drinkSlug != OTHER_DRINK && edit.customDrinkName != null -> EditIssue.DrinkNameNotAllowed
    else -> null
}

private fun outOfRange(rating: PostRating) = listOf(rating.coffee, rating.service, rating.place).any { it !in 1..5 }

enum class ReportIssue { Empty, TooLong }

fun validateReport(text: String): ReportIssue? = when {
    text.trim().isEmpty() -> ReportIssue.Empty
    text.trim().length > MAX_REPORT_LENGTH -> ReportIssue.TooLong
    else -> null
}

enum class PublicationLabel { Private, Pending, Published, Rejected, Public, Unknown }

fun CommunityPost.publicationLabel(): PublicationLabel = if (visibility == PostVisibility.Private) PublicationLabel.Private else when (moderation) {
    PostModeration.Pending -> PublicationLabel.Pending
    PostModeration.Approved -> PublicationLabel.Published
    PostModeration.Rejected -> PublicationLabel.Rejected
    PostModeration.NotSubmitted -> PublicationLabel.Public
    PostModeration.Unknown -> PublicationLabel.Unknown
}

/** An edited or hidden post leaves the public timeline unless it is public and approved. */
fun isWithdrawnFromPublic(updated: CommunityPost): Boolean =
    updated.visibility != PostVisibility.Public || updated.moderation != PostModeration.Approved

/** The website link of a post, null unless it is public and approved: anyone else opening the link of another post gets «unavailable». */
fun CommunityPost.shareLink(): String? = "https://coffeepeek.by/check-ins/$id".takeIf { id.isNotBlank() && !isWithdrawnFromPublic(this) }

/** Keeps the server's publication order: known posts are refreshed in place, new ones are appended. */
fun appendTimeline(current: List<CommunityPost>, incoming: List<CommunityPost>): List<CommunityPost> {
    val updates = incoming.associateBy(CommunityPost::id)
    val known = current.mapTo(mutableSetOf(), CommunityPost::id)
    return current.map { updates[it.id] ?: it } + incoming.filter { known.add(it.id) }
}

/**
 * [signedIn] is null until the first session answer. Own posts are every post of the personal timeline, or the public ones whose author
 * slug equals the canonical [ownSlug].
 */
fun ownsPost(post: CommunityPost, signedIn: Boolean?, personalTimeline: Boolean, ownSlug: String?): Boolean =
    signedIn == true && (personalTimeline || (ownSlug != null && post.author.slug == ownSlug))

/**
 * Only the public timeline offers blocking. A guest may open the sign-in prompt; a signed-in viewer waits for the canonical own slug,
 * because public author ids differ from authentication ids.
 */
fun canBlockAuthor(signedIn: Boolean?, personalTimeline: Boolean, ownSlug: String?, owned: Boolean): Boolean =
    !personalTimeline && when (signedIn) {
        false -> true
        true -> ownSlug != null && !owned
        null -> false
    }
