package com.coffeepeek.feature.contentreport.impl.ui

import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.contentreport.domain.model.*
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import com.coffeepeek.feature.contentreport.impl.ui.compose.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class ContentReportViewModel(
    private val target: ContentReportTarget, preview: Boolean, loggedIn: Boolean, accessAvailable: Boolean,
    private val repository: ContentReportRepository, private val isCurrent: () -> Boolean,
) : MviViewModel<ContentReportState, ContentReportAction, ContentReportEventEnvelope>(
    ContentReportState(isPreview = preview, isCheckIn = target.kind == ContentReportKind.CheckIn,
        isLoggedIn = loggedIn, accessAvailable = accessAvailable),
) {
    private var signInRequested = false
    override suspend fun handleActionInternal(action: ContentReportAction) {
        when (action) {
            is ContentReportAction.TextChanged -> if (currentState.canEdit) {
                updateState { copy(text = limitReportText(action.text), error = null) }
            }
            is ContentReportAction.Submit -> submit(action.owner)
            is ContentReportAction.Back -> sendEvent(ContentReportEventEnvelope(ContentReportEvent.Back, action.owner))
            is ContentReportAction.RetryAccess -> if (!currentState.accessAvailable) {
                sendEvent(ContentReportEventEnvelope(ContentReportEvent.RetryAccess, action.owner))
            }
            ContentReportAction.Resume -> signInRequested = false
        }
    }

    private suspend fun submit(owner: Any) {
        val state = currentState
        if (!state.canEdit || signInRequested || !isCurrent()) return
        val reason = state.text.trim()
        if (!validReportReason(reason)) {
            updateState { copy(error = ContentReportError(ContentReportProblem.InvalidInput)) }
        } else if (!state.isPreview && target.id.isBlank()) {
            updateState { copy(error = ContentReportError(ContentReportProblem.NotFound)) }
        } else if (state.isPreview) {
            updateState { copy(isSubmitted = true, text = "", error = null) }
        } else if (!state.isLoggedIn) {
            signInRequested = true
            updateState { copy(error = ContentReportError(ContentReportProblem.SignInRequired)) }
            sendEvent(ContentReportEventEnvelope(ContentReportEvent.SignIn, owner))
        } else {
            updateState { copy(isSubmitting = true, error = null) }
            send(reason, owner)
        }
    }

    private suspend fun send(reason: String, owner: Any) {
        try {
            repository.submit(target, reason).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (isCurrent()) updateState { copy(isSubmitted = true, text = "") }
        } catch (cancelled: CancellationException) {
            updateState { copy(deliveryUnconfirmed = true) }
            throw cancelled
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            if (isCurrent()) failure(error, owner)
        } finally {
            updateState { copy(isSubmitting = false) }
        }
    }

    private suspend fun failure(error: Exception, owner: Any) {
        val failure = error as? ContentReportFailure ?: ContentReportFailure(ContentReportProblem.Unconfirmed)
        updateState { copy(error = ContentReportError(failure.problem, failure.serverMessage).takeUnless { it.problem == ContentReportProblem.Unconfirmed },
            deliveryUnconfirmed = failure.problem == ContentReportProblem.Unconfirmed) }
        if (failure.problem == ContentReportProblem.SignInRequired) {
            signInRequested = true
            sendEvent(ContentReportEventEnvelope(ContentReportEvent.SignIn, owner))
        }
    }
}

/** Preserve UTF-16 limit while keeping a final emoji's surrogate pair intact. */
internal fun limitReportText(text: String): String {
    if (text.length <= CONTENT_REPORT_MAX_LENGTH) return text
    val limit = CONTENT_REPORT_MAX_LENGTH
    val end = if (text[limit - 1].isHighSurrogate() && text[limit].isLowSurrogate()) limit - 1 else limit
    return text.take(end)
}
