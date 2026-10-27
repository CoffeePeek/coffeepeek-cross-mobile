package com.coffeepeek.feature.community.impl.post

import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.rules.ReportIssue
import com.coffeepeek.feature.community.domain.rules.canBlockAuthor
import com.coffeepeek.feature.community.domain.rules.isWithdrawnFromPublic
import com.coffeepeek.feature.community.domain.rules.ownsPost
import com.coffeepeek.feature.community.impl.timeline.ActionMessage

/** [Unavailable]: the server does not show this post to the viewer, or its author is blocked. [Failed]: it could not be read. */
internal enum class PostError { Unavailable, Failed }

internal data class PostState(
    /** Null until the first session answer. */
    val isLoggedIn: Boolean? = null,
    val post: CommunityPost? = null,
    val isLoading: Boolean = false,
    val error: PostError? = null,
    val ownAuthorSlug: String? = null,
    val isVoting: Boolean = false,
    val isReportOpen: Boolean = false,
    val reportText: String = "",
    val reportIssue: ReportIssue? = null,
    /** What the server answered when the report failed; null with [reportFailed] means the default text. */
    val reportFailure: String? = null,
    val reportFailed: Boolean = false,
    val isReporting: Boolean = false,
) {
    val isMutating: Boolean get() = isVoting || isReporting
    val isOwn: Boolean get() = post != null && ownsPost(post, isLoggedIn, personalTimeline = false, ownSlug = ownAuthorSlug)

    /** Likes, reports and blocking apply to a post everyone can see; an owner may also open a private or unapproved one. */
    val isPublished: Boolean get() = post != null && !isWithdrawnFromPublic(post)
    val canBlockAuthor: Boolean get() = isPublished && canBlockAuthor(isLoggedIn, personalTimeline = false, ownSlug = ownAuthorSlug, owned = isOwn)
}

internal sealed interface PostAction {
    data object Retry : PostAction
    data object ToggleHelpful : PostAction
    data object OpenReport : PostAction
    data class ReportText(val text: String) : PostAction
    data object SubmitReport : PostAction
    data object DismissReport : PostAction
}

internal sealed interface PostEvent {
    /** The screen must open the sign-in destination. */
    data object SignInRequested : PostEvent
    data class Message(val message: ActionMessage) : PostEvent
}
