package com.coffeepeek.feature.contentreport.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.contentreport.domain.model.ContentReportKind
import com.coffeepeek.feature.contentreport.domain.model.ContentReportProblem
import com.coffeepeek.feature.contentreport.domain.model.ContentReportTarget
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import com.coffeepeek.feature.contentreport.impl.ui.ContentReportViewModel
import com.coffeepeek.feature.contentreport.impl.ui.compose.model.ContentReportAction
import com.coffeepeek.feature.contentreport.impl.ui.compose.model.ContentReportEvent
import com.coffeepeek.feature.contentreport.impl.ui.compose.model.ContentReportState
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/** Configuration comes from the route and the app's account-bound report host. */
data class NativeContentReportInput(
    val id: String,
    val isCheckIn: Boolean,
    val preview: Boolean,
    val loggedIn: Boolean,
    val accessAvailable: Boolean,
)

data class NativeContentReportState(
    val text: String,
    val preview: Boolean,
    val accessAvailable: Boolean,
    val canEdit: Boolean,
    val submitting: Boolean,
    val submitted: Boolean,
    val deliveryUnconfirmed: Boolean,
    val errorKey: String?,
    val serverMessage: String?,
)

enum class NativeContentReportEvent { Back, SignIn, RetryAccess }

/** Owns the same report form ViewModel used by the Compose entry. */
class NativeContentReport(
    input: NativeContentReportInput,
    repository: ContentReportRepository,
    isCurrent: () -> Boolean,
) {
    private val owner = Any()
    private val model = reportModel(input, repository, isCurrent)
    private val store = ViewModelStore().apply { put("content-report", model) }

    val states = model.state.map { it.nativeSnapshot() }
    val events = model.events.mapNotNull { it.forOwner(owner)?.nativeEvent() }

    fun currentState() = model.state.value.nativeSnapshot()
    fun changeText(text: String) = model.onAction(ContentReportAction.TextChanged(text))
    fun submit() = model.onAction(ContentReportAction.Submit(owner))
    fun back() = model.onAction(ContentReportAction.Back(owner))
    fun retryAccess() = model.onAction(ContentReportAction.RetryAccess(owner))
    fun resume() = model.onAction(ContentReportAction.Resume)
    fun close() = store.clear()
}

private fun reportModel(
    input: NativeContentReportInput, repository: ContentReportRepository, isCurrent: () -> Boolean,
): ContentReportViewModel {
    val kind = if (input.isCheckIn) ContentReportKind.CheckIn else ContentReportKind.Review
    return ContentReportViewModel(
        ContentReportTarget(kind, input.id), input.preview, input.loggedIn, input.accessAvailable, repository, isCurrent,
    )
}

private fun ContentReportEvent.nativeEvent() = when (this) {
    ContentReportEvent.Back -> NativeContentReportEvent.Back
    ContentReportEvent.SignIn -> NativeContentReportEvent.SignIn
    ContentReportEvent.RetryAccess -> NativeContentReportEvent.RetryAccess
}

private fun ContentReportState.nativeSnapshot() = NativeContentReportState(
    text, isPreview, accessAvailable, canEdit, isSubmitting, isSubmitted, deliveryUnconfirmed,
    error?.let { errorKey(it.problem) }, error?.serverMessage?.takeIf(String::isNotBlank),
)

private fun ContentReportState.errorKey(problem: ContentReportProblem): String = when (problem) {
    ContentReportProblem.InvalidInput -> "content_report_validation"
    ContentReportProblem.SignInRequired ->
        if (isLoggedIn && isCheckIn) "content_report_check_in_sign_in" else "content_report_sign_in"
    ContentReportProblem.Forbidden ->
        if (isCheckIn) "content_report_forbidden" else "content_report_failed"
    ContentReportProblem.NotFound ->
        if (isCheckIn) "content_report_check_in_missing" else "content_report_review_missing"
    ContentReportProblem.RateLimited ->
        if (isCheckIn) "content_report_check_in_failed" else "content_report_rate_limit"
    ContentReportProblem.Rejected, ContentReportProblem.Unconfirmed ->
        if (isCheckIn) "content_report_check_in_failed" else "content_report_failed"
}
