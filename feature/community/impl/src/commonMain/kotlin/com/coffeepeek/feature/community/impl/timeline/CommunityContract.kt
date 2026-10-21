package com.coffeepeek.feature.community.impl.timeline

import com.coffeepeek.feature.community.api.FeedItemAction
import com.coffeepeek.feature.community.api.FeedTrigger
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.DrinkOption
import com.coffeepeek.feature.community.domain.model.FeedFilters
import com.coffeepeek.feature.community.domain.model.PostEdit
import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.domain.rules.EditIssue
import com.coffeepeek.feature.community.domain.rules.ReportIssue
import com.coffeepeek.feature.community.domain.rules.canBlockAuthor
import com.coffeepeek.feature.community.domain.rules.validateEdit
import com.coffeepeek.feature.community.domain.rules.ownsPost

internal enum class CommunityTimeline { Public, Mine }

internal enum class TimelineError { LoadFailed, BlocksFailed }

internal enum class MessageKind { VoteFailed, VisibilityFailed, ReportSent, EditSaved, EditSentForReview }

/** A short notice for the snackbar; [serverMessage] is shown instead of the default text when the server sent one. */
internal data class ActionMessage(val kind: MessageKind, val serverMessage: String? = null)

/** Why the edit form shows an error: a rule of the form, or what the server answered when saving (null: the default text). */
internal sealed interface EditError {
    data class Invalid(val issue: EditIssue) : EditError
    data class Server(val message: String?) : EditError
}

internal data class PostEditState(
    val source: CommunityPost,
    val text: String = source.note,
    val rating: PostRating = source.rating ?: PostRating(0, 0, 0),
    val drinkSlug: String? = source.drink?.slug,
    val customDrinkName: String? = source.drink?.customName,
    val error: EditError? = null,
) {
    fun input() = PostEdit(text.trim(), rating.takeUnless { it == PostRating(0, 0, 0) }, drinkSlug, customDrinkName?.trim())
    fun issue(): EditIssue? = validateEdit(input())
}

internal data class CommunityState(
    /** Null until the first session answer. */
    val isLoggedIn: Boolean? = null,
    /** Bumped when the account or the timeline scope changes; late answers for an older generation are dropped. */
    val sessionGeneration: Long = 0,
    val items: List<CommunityPost> = emptyList(),
    val firstPageCount: Int? = null,
    val timeline: CommunityTimeline = CommunityTimeline.Public,
    val filters: FeedFilters = FeedFilters(),
    val nextCursor: String? = null,
    val publishedAt: Map<String, String> = emptyMap(),
    val ownAuthorSlug: String? = null,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: TimelineError? = null,
    val failedPage: Int? = null,
    val helpfulId: String? = null,
    /** The cursor is no longer valid: the retry starts from the first page. */
    val restartPagination: Boolean = false,
    val editing: PostEditState? = null,
    val isSaving: Boolean = false,
    val changingVisibilityId: String? = null,
    val drinks: List<DrinkOption> = emptyList(),
    val drinksFailed: Boolean = false,
    val reportingId: String? = null,
    val reportText: String = "",
    val reportIssue: ReportIssue? = null,
    /** What the server answered when the report failed; null with [reportFailed] means the default text. */
    val reportFailure: String? = null,
    val reportFailed: Boolean = false,
    val isReporting: Boolean = false,
) {
    val isMutating: Boolean get() = isSaving || changingVisibilityId != null || helpfulId != null || isReporting
    fun owns(post: CommunityPost): Boolean = ownsPost(post, isLoggedIn, timeline == CommunityTimeline.Mine, ownAuthorSlug)
    fun canBlockAuthor(post: CommunityPost): Boolean = canBlockAuthor(isLoggedIn, timeline == CommunityTimeline.Mine, ownAuthorSlug, owns(post))
}

internal sealed interface CommunityAction {
    data class Refresh(val trigger: FeedTrigger? = null) : CommunityAction
    data class LoadMore(val track: Boolean = true) : CommunityAction
    data object Retry : CommunityAction
    data class SelectTimeline(val timeline: CommunityTimeline) : CommunityAction
    data class SetFilters(val filters: FeedFilters) : CommunityAction
    data class ToggleHelpful(val id: String) : CommunityAction
    data class ItemInteracted(val action: FeedItemAction, val id: String) : CommunityAction
    data class Edit(val id: String) : CommunityAction
    data class EditText(val text: String) : CommunityAction
    data class EditRating(val rating: PostRating) : CommunityAction
    data class EditDrink(val slug: String?, val customName: String?) : CommunityAction
    data object LoadDrinks : CommunityAction
    data object SaveEdit : CommunityAction
    data object DismissEdit : CommunityAction
    data class ToggleVisibility(val id: String) : CommunityAction
    data class OpenReport(val id: String) : CommunityAction
    data class ReportText(val text: String) : CommunityAction
    data object SubmitReport : CommunityAction
    data object DismissReport : CommunityAction
}

internal sealed interface CommunityEvent {
    /** The screen must open the sign-in destination. */
    data object SignInRequested : CommunityEvent
    data class Message(val message: ActionMessage) : CommunityEvent
}
